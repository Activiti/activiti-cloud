/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starter.tests.runtime;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.cloud.starter.tests.services.audit.AuditConsumerStreamHandler;
import org.activiti.cloud.starter.tests.services.audit.AuditProducerIT;
import org.activiti.cloud.starter.tests.services.audit.ServicesAuditITConfiguration;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.services.connectors.conf.ConnectorImplementationsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(
    classes = { RuntimeITConfiguration.class, ServicesAuditITConfiguration.class },
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
public abstract class AbstractMQServiceTaskIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMQServiceTaskIT.class);

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected IdentityTokenProducer identityTokenProducer;

    @Autowired
    protected ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    protected TaskRestTemplate taskRestTemplate;

    @Autowired
    protected ConnectorImplementationsProvider connectorImplementationsProvider;

    @Autowired
    protected BindingServiceProperties bindingServiceProperties;

    @Autowired
    private AuditConsumerStreamHandler auditConsumer;

    @Autowired
    private ServiceTaskConsumerHandler serviceTaskConsumerHandler;

    @Value("${activiti.identity.test-user:hruser}")
    protected String keycloakTestUser;

    @BeforeEach
    public void setUp() {
        identityTokenProducer.withTestUser(keycloakTestUser);
    }

    @Test
    public void shouldProvideConnectorImplementations() {
        //given

        //when
        List<String> destinations = connectorImplementationsProvider.getImplementations();

        //then
        assertThat(destinations)
            .contains(
                "mealsConnector",
                "rest.GET",
                "perfromBusinessTask",
                "anyImplWithoutHandler",
                "payment",
                "Constants Connector.constantsActionName",
                "Variable Mapping Connector.variableMappingActionName",
                "miCloudConnector"
            );
    }

    @Test
    public void shouldContinueExecution() {
        //given

        CustomPojo customPojo = new CustomPojo();
        customPojo.setField1("field1");

        CustomPojoAnnotated customPojoAnnotated = new CustomPojoAnnotated();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", "John");
        variables.put("lastName", "Smith");
        variables.put("age", 19);
        variables.put("boolVar", true);
        variables.put("customPojo", customPojo);
        variables.put("customPojoAnnotated", customPojoAnnotated);

        //when
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey(
            "MQServiceTaskProcess",
            "businessKey",
            variables
        );
        assertThat(procInst).isNotNull();

        //then
        await("the execution should arrive in the human tasks which follows the service task")
            .untilAsserted(() -> {
                List<Task> tasks = taskService
                    .createTaskQuery()
                    .processInstanceId(procInst.getProcessInstanceId())
                    .list();
                assertThat(tasks).isNotNull();
                assertThat(tasks).extracting(Task::getName).containsExactly("Schedule meeting after service");
            });

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();

        // the variable "age" should be updated based on ServiceTaskConsumerHandler.receive
        Map<String, Object> updatedVariables = runtimeService.getVariables(procInst.getId());
        assertThat(updatedVariables)
            .containsEntry("firstName", "John")
            .containsEntry("lastName", "Smith")
            .containsEntry("age", 20)
            .containsEntry("boolVar", false);

        //engine can resolve annotated pojo in var to correct type but not without annotation
        assertThat(updatedVariables.get("customPojo").getClass()).isEqualTo(CustomPojo.class);
        assertThat(updatedVariables.get("customPojoAnnotated").getClass()).isEqualTo(CustomPojoAnnotated.class);

        assertThat(updatedVariables.get("customPojoTypeInConnector"))
            .isEqualTo("Type of customPojo var in connector is " + LinkedHashMap.class);
        assertThat(updatedVariables.get("customPojoField1InConnector"))
            .isEqualTo("Value of field1 on customPojo is field1");
        assertThat(updatedVariables.get("customPojoAnnotatedTypeInConnector"))
            .isEqualTo("Type of customPojoAnnotated var in connector is " + LinkedHashMap.class);

        //should be able to complete the process
        //when
        taskService.complete(tasks.get(0).getId());

        //then
        List<ProcessInstance> processInstances = runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(procInst.getId())
            .list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void integrationContextShouldBeDeletedWhenTheTaskIsCancelled() {
        //given
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("MQServiceTaskWithBoundaryProcess");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
        assertThat(tasks).isEmpty();

        //when boundary is triggered
        runtimeService.signalEventReceived("goPlanB");

        //then the exception path is taken
        tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("Execute plan B");

        //when the task related to the exception path is executed
        taskService.complete(tasks.get(0).getId());

        //the process should finish
        List<ProcessInstance> processInstances = runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(procInst.getId())
            .list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void shouldHandleVariableMappings() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("connectorVarMapping")
                .withBusinessKey("businessKey")
                .build()
        );

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(
                    processInstanceResponseEntity
                );

                //then
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .containsOnly(
                        tuple("name", "outName"), //mapped from connector outputs based on extension mappings
                        tuple("age", 25), //mapped from connector outputs based on extension mappings
                        tuple("input_unmapped_variable_with_matching_name", "inTest"), //kept unchanging because no connector output is updating it
                        tuple("input_unmapped_variable_with_non_matching_connector_input_name", "inTest"), //kept unchanging because no connector output is updating it
                        tuple("nickName", "testName"), //kept unchanging because no connector output is updating it
                        tuple("out_unmapped_variable_matching_name", "default"), //not present in extension mappings, hence not updated although
                        // the process variable have the same name as the connector output
                        tuple("output_unmapped_variable_with_non_matching_connector_output_name", "default"),
                        tuple("outVarFromJsonExpression", "Tower of London"),
                        tuple("outVarFromListExpression", "Peter")
                    ); //kept unchanging because no connector output is updating it
            });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(
            processInstanceResponseEntity
        );
        assertThat(tasks.getBody()).isNotNull();
        assertThat(tasks.getBody().getContent()).extracting(CloudTask::getName).containsExactly("My user task");
    }

    @Test
    public void should_supportVariableMappingAfterLoopingBack() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start().withProcessDefinitionKey("Process_N4qkN051N").build()
        );

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(
            processInstanceResponseEntity
        );
        assertThat(tasks.getBody().getContent()).extracting(CloudTask::getName).containsExactly("Enter values");

        //when the task completes with a variable value causing a loop back
        taskRestTemplate.complete(
            tasks.getBody().iterator().next(),
            TaskPayloadBuilder.complete().withVariable("formInput", "provided-it1").build()
        );

        //then process loops back to the first task
        waitForTaskOnProcessInstance(processInstanceResponseEntity, "Enter values");
        ResponseEntity<CollectionModel<CloudVariableInstance>> variables = processInstanceRestTemplate.getVariables(
            processInstanceResponseEntity
        );
        assertThat(variables.getBody())
            .extracting(VariableInstance::getName, VariableInstance::getValue)
            .containsExactly(tuple("providedValue", "provided-it1"));

        //when the task completes with a variable value not causing a loop back
        tasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        taskRestTemplate.complete(
            tasks.getBody().iterator().next(),
            TaskPayloadBuilder.complete().withVariable("formInput", "go").build()
        );

        //then the process reaches the next task
        waitForTaskOnProcessInstance(processInstanceResponseEntity, "Wait");
        variables = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
        assertThat(variables.getBody())
            .extracting(VariableInstance::getName, VariableInstance::getValue)
            .containsExactly(tuple("providedValue", "go"));
    }

    private void waitForTaskOnProcessInstance(
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity,
        String name
    ) {
        await()
            .untilAsserted(() ->
                assertThat(processInstanceRestTemplate.getTasks(processInstanceResponseEntity).getBody().getContent())
                    .extracting(CloudTask::getName)
                    .containsExactly(name)
            );
    }

    @Test
    public void shouldHandleConstants() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("connectorConstants")
                .withBusinessKey("businessKey")
                .build()
        );

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(
                    processInstanceResponseEntity
                );

                //then
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .containsOnly(
                        tuple("name", "outName"), //mapped from connector outputs based on extension mappings
                        tuple("age", 25),
                        tuple("_constant_value_", "myConstantValue")
                    );
            });
    }

    /**
     * Covers https://github.com/Activiti/Activiti/issues/2736
     *
     * @see ServiceTaskConsumerHandler#receiveRestConnector(IntegrationRequest, Map) for headers assertions
     */
    @Test
    public void integrationRequestShouldAlwaysHaveProcessDefinitionVersionSet() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("process-f0d643a4-27d7-474f-b71f-4d7f04989843")
                .withBusinessKey("businessKey")
                .build()
        );

        CloudTask task = getTaskToExecute(processInstanceResponseEntity);
        taskRestTemplate.claim(task);
        taskRestTemplate.complete(task);

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(
                    processInstanceResponseEntity
                );

                //then
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName, CloudVariableInstance::getValue)
                    .containsOnly(tuple("restResult", "fromConnector")); //kept unchanging because no connector output is updating it
            });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(
            processInstanceResponseEntity
        );
        assertThat(tasks.getBody()).isNotNull();
        assertThat(tasks.getBody().getContent()).extracting(CloudTask::getName).containsExactly("Result Form Task");
    }

    private CloudTask getTaskToExecute(ResponseEntity<CloudProcessInstance> processInstanceResponseEntity) {
        ResponseEntity<PagedModel<CloudTask>> availableTasks = processInstanceRestTemplate.getTasks(
            processInstanceResponseEntity
        );
        assertThat(availableTasks).isNotNull();
        assertThat(availableTasks.getBody()).isNotEmpty();
        return availableTasks.getBody().getContent().iterator().next();
    }

    @Test
    public void multiInstance_should_collectSpecifiedVariable_when_dataItemIsSet() throws InterruptedException {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("multi-instance-service-task-result-collection-data-item")
                .build()
        );

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(
                    processInstance
                );

                //then
                Collection<CloudVariableInstance> variables = variablesResponse.getBody().getContent();
                assertThat(variables)
                    .extracting(VariableInstance::getName, VariableInstance::getValue)
                    .contains(tuple("meals", asList("pizza", "pasta")));
            });
    }

    @Test
    public void multiInstance_should_collectAllVariables_when_noDataItem() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("multi-instance-service-task-result-collection-all")
                .build()
        );

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(
                    processInstance
                );

                //then
                Collection<CloudVariableInstance> variables = variablesResponse.getBody().getContent();
                assertThat(variables)
                    .extracting(VariableInstance::getName, VariableInstance::getValue)
                    .contains(
                        tuple(
                            "miResult",
                            asList(Map.of("meal", "pizza", "size", "small"), Map.of("meal", "pasta", "size", "medium"))
                        )
                    );
            });
    }

    @Test
    void should_beAbleToExecuteMultiInstanceServiceTasksAndNotMultiInstantiatedServiceTasksWithoutRaceConditions()
        throws Exception {
        //given
        CompletableFuture<ProcessInstance> singleInstanceCompletableFuture = CompletableFuture.supplyAsync(() ->
            runtimeService.startProcessInstanceByKey(
                "serviceTaskSingleInstanceRaceConditionWithOtherProcessWithMultiInstance"
            )
        );

        CompletableFuture<ProcessInstance> multiInstanceCompletableFuture = CompletableFuture.supplyAsync(() -> {
            waitForSingleInstanceToStart();
            ProcessInstance processWithMultiInstance = runtimeService.startProcessInstanceByKey(
                "miSequentialServiceTaskRaceCondition"
            );
            waitForFirstMultiInstanceToComplete(processWithMultiInstance);

            //when
            resumeExecutionOfSingleInstance();
            return processWithMultiInstance;
        });

        //then
        ProcessInstance singleProcessInstance = singleInstanceCompletableFuture.get(10, TimeUnit.SECONDS);
        ProcessInstance multiInstanceProcess = multiInstanceCompletableFuture.get(10, TimeUnit.SECONDS);
        await()
            .untilAsserted(() ->
                assertThat(auditConsumer.getAllReceivedEvents(CloudProcessCompletedEvent.class))
                    .extracting(CloudRuntimeEvent::getProcessInstanceId, CloudRuntimeEvent::getProcessDefinitionKey)
                    .contains(
                        tuple(singleProcessInstance.getId(), singleProcessInstance.getProcessDefinitionKey()),
                        tuple(multiInstanceProcess.getId(), multiInstanceProcess.getProcessDefinitionKey())
                    )
            );
    }

    private void resumeExecutionOfSingleInstance() {
        serviceTaskConsumerHandler.getMultiInstanceLatch().countDown();
        LOGGER.info("Multi-instance latch counted down . Thread: {}", Thread.currentThread().threadId());
    }

    private void waitForFirstMultiInstanceToComplete(ProcessInstance processWithMultiInstance) {
        LOGGER.info(
            "Waiting for the first integration result for multi instance before counting down multi-instance latch... Thread: {}",
            Thread.currentThread().threadId()
        );
        await()
            .untilAsserted(() ->
                assertThat(auditConsumer.getAllReceivedEvents(CloudIntegrationResultReceivedEvent.class))
                    .extracting(CloudIntegrationResultReceivedEvent::getProcessInstanceId)
                    .contains(processWithMultiInstance.getId())
            );
    }

    private void waitForSingleInstanceToStart() {
        try {
            LOGGER.info(
                "Waiting for single instance latch to be counted down... Thread: {}",
                Thread.currentThread().threadId()
            );
            boolean conditionReached = serviceTaskConsumerHandler.getSingleInstanceLatch().await(5, TimeUnit.SECONDS);
            assertThat(conditionReached).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
