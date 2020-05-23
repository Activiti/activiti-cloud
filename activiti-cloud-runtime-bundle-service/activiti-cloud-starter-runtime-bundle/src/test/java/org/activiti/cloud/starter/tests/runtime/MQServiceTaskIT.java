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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class MQServiceTaskIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Value("${activiti.keycloak.test-user:hruser}")
    protected String keycloakTestUser;

    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
    }

    @Test
    public void shouldContinueExecution() {
        //given

        CustomPojo customPojo = new CustomPojo();
        customPojo.setField1("field1");

        CustomPojoAnnotated customPojoAnnotated = new CustomPojoAnnotated();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
            "John");
        variables.put("lastName",
            "Smith");
        variables.put("age",
            19);
        variables.put("boolVar",
            true);
        variables.put("customPojo",
            customPojo
        );
        variables.put("customPojoAnnotated",
            customPojoAnnotated);

        //when
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("MQServiceTaskProcess",
            "businessKey",
            variables);
        assertThat(procInst).isNotNull();

        //then
        await("the execution should arrive in the human tasks which follows the service task")
            .untilAsserted(() -> {
                    List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
                    assertThat(tasks).isNotNull();
                    assertThat(tasks).extracting(Task::getName).containsExactly("Schedule meeting after service");
                }
            );

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();

        // the variable "age" should be updated based on ServiceTaskConsumerHandler.receive
        Map<String, Object> updatedVariables = runtimeService.getVariables(procInst.getId());
        assertThat(updatedVariables)
            .containsEntry("firstName",
                "John")
            .containsEntry("lastName",
                "Smith")
            .containsEntry("age",
                20)
            .containsEntry("boolVar",
                false);

        //engine can resolve annotated pojo in var to correct type but not without annotation
        assertThat(updatedVariables.get("customPojo").getClass()).isEqualTo(CustomPojo.class);
        assertThat(updatedVariables.get("customPojoAnnotated").getClass()).isEqualTo(CustomPojoAnnotated.class);

        assertThat(updatedVariables.get("customPojoTypeInConnector")).isEqualTo("Type of customPojo var in connector is " + LinkedHashMap.class);
        assertThat(updatedVariables.get("customPojoField1InConnector")).isEqualTo("Value of field1 on customPojo is field1");
        assertThat(updatedVariables.get("customPojoAnnotatedTypeInConnector")).isEqualTo("Type of customPojoAnnotated var in connector is " + LinkedHashMap.class);

        //should be able to complete the process
        //when
        taskService.complete(tasks.get(0).getId());

        //then
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst.getId()).list();
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
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst.getId()).list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void shouldHandleVariableMappings() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("connectorVarMapping")
                .withBusinessKey("businessKey")
                .build());

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);

            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("name",
                    "outName"), //mapped from connector outputs based on extension mappings
                    tuple("age",
                        25),        //mapped from connector outputs based on extension mappings
                    tuple("input_unmapped_variable_with_matching_name",
                        "inTest"), //kept unchanging because no connector output is updating it
                    tuple("input_unmapped_variable_with_non_matching_connector_input_name",
                        "inTest"), //kept unchanging because no connector output is updating it
                    tuple("nickName",
                        "testName"),//kept unchanging because no connector output is updating it
                    tuple("out_unmapped_variable_matching_name",
                        "default"),//not present in extension mappings, hence not updated although
                    // the process variable have the same name as the connector output
                    tuple("output_unmapped_variable_with_non_matching_connector_output_name",
                        "default"),
                    tuple("outVarFromJsonExpression", "Tower of London"),
                    tuple("outVarFromListExpression",
                        "Peter"));//kept unchanging because no connector output is updating it

        });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        assertThat(tasks.getBody()).isNotNull();
        assertThat(tasks.getBody().getContent())
            .extracting(CloudTask::getName)
            .containsExactly("My user task");
    }

    @Test
    public void shouldHandleConstants() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("connectorConstants")
                .withBusinessKey("businessKey")
                .build());

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);

            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("name",
                    "outName"), //mapped from connector outputs based on extension mappings
                    tuple("age",
                        25),
                    tuple("_constant_value_",
                        "myConstantValue"));

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
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("process-f0d643a4-27d7-474f-b71f-4d7f04989843")
                .withBusinessKey("businessKey")
                .build());

        CloudTask task = getTaskToExecute(processInstanceResponseEntity);
        taskRestTemplate.claim(task);
        taskRestTemplate.complete(task);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);

            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("restResult",
                    "fromConnector"));//kept unchanging because no connector output is updating it
        });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        assertThat(tasks.getBody()).isNotNull();
        assertThat(tasks.getBody().getContent())
            .extracting(CloudTask::getName)
            .containsExactly("Result Form Task");
    }

    private CloudTask getTaskToExecute(ResponseEntity<CloudProcessInstance> processInstanceResponseEntity) {
        ResponseEntity<PagedModel<CloudTask>> availableTasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        assertThat(availableTasks).isNotNull();
        assertThat(availableTasks.getBody()).isNotEmpty();
        return availableTasks.getBody().getContent().iterator().next();
    }
}
