/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.starter.tests;

import static org.activiti.cloud.services.query.events.handlers.BaseBPMNActivityEventHandler.SERVICE_TASK_TYPE;
import static org.activiti.cloud.services.query.model.IntegrationContextEntity.ERROR_MESSAGE_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.CloudServiceTask;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.StringUtils;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test-admin.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class QueryAdminProcessServiceTasksIT {

    private static final String ERROR_MESSAGE =
        "An error occurred consuming ACS API with inputs {targetFolder={}, action=CREATE_FILE}. Cause: [405] during [GET] to [https://aae-3734-env.envalfresco.com/alfresco/api/-default-/public/alfresco/versions/1/nodes/] [NodesApiClient#getNode(String,List,String,List)]: [{\"error\":{\"errorKey\":\"framework.exception.UnsupportedResourceOperation\",\"statusCode\":405,\"briefSummary\":\"09070282 The operation is unsupported\",\"stackTrace\":\"For security reasons the stack trace is no longer displayed, but the property is kept for previous versions\",\"descriptionURL\":\"https://api-explorer.alfresco.com\"}}]";

    private static final ParameterizedTypeReference<PagedModel<CloudServiceTask>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<CloudServiceTask>>() {};
    private static final ParameterizedTypeReference<CloudServiceTask> SINGLE_TASK_RESPONSE_TYPE = new ParameterizedTypeReference<CloudServiceTask>() {};
    private static final ParameterizedTypeReference<CloudIntegrationContext> SINGLE_INT_CONTEXT_RESPONSE_TYPE = new ParameterizedTypeReference<CloudIntegrationContext>() {};

    private static final String PROC_URL = "/admin/v1/process-instances/{processInstanceId}/service-tasks";
    private static final String SERVICE_TASKS_URL = "/admin/v1/service-tasks";
    private static final String WITH_STATUS = "?status={status}";

    private static final String ELEMENT_ID = "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94";
    private static final String ROOT_PROCESS_INSTANCE_ID = "56824d90-cd3e-45fc-bbfc-32f91dab775f";
    private static final String EXECUTION_ID = "95d8752a-d2b7-4acb-8eda-5fad2d952bed";
    private static final String SERVICE_TASK_NAME = "Service Task";

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessModelRepository processModelRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private BPMNActivityRepository bpmnActivityRepository;

    @Autowired
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @Autowired
    private IntegrationContextRepository integrationContextRepository;

    @Autowired
    private MyProducer producer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String processDefinitionId = UUID.randomUUID().toString();

    private EventsAggregator eventsAggregator;

    @BeforeEach
    public void setUp() throws IOException {
        identityTokenProducer.withTestUser("hradmin");

        eventsAggregator = new EventsAggregator(producer);

        //given
        deployFirstProcessDefinition();
    }

    private void deployFirstProcessDefinition() throws IOException {
        ProcessDefinitionImpl firstProcessDefinition = new ProcessDefinitionImpl();
        firstProcessDefinition.setId(processDefinitionId);
        firstProcessDefinition.setKey("mySimpleProcess");
        firstProcessDefinition.setName("My Simple Process");

        CloudProcessDeployedEventImpl firstProcessDeployedEvent = new CloudProcessDeployedEventImpl(
            firstProcessDefinition
        );
        firstProcessDeployedEvent.setProcessModelContent(
            Files.readString(Paths.get("src/test/resources/parse-for-test/SimpleProcess.bpmn20.xml"))
        );

        producer.send(firstProcessDeployedEvent);
    }

    @AfterEach
    public void tearDown() {
        processModelRepository.deleteAll();
        processDefinitionRepository.deleteAll();
        processInstanceRepository.deleteAll();
        integrationContextRepository.deleteAll();
        bpmnActivityRepository.deleteAll();
        bpmnSequenceFlowRepository.deleteAll();
    }

    @Test
    public void shouldGetProcessInstanceServiceTasks() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    PROC_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    process.getId()
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getActivityType)
                    .contains(SERVICE_TASK_TYPE);
            });
    }

    @Test
    public void shouldGetProcessInstanceServiceTasksByStatus() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    PROC_URL + WITH_STATUS,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    process.getId(),
                    CloudBPMNActivity.BPMNActivityStatus.STARTED
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getStatus, CloudServiceTask::getActivityType)
                    .contains(tuple(CloudBPMNActivity.BPMNActivityStatus.STARTED, SERVICE_TASK_TYPE));
            });

        // and given
        sendIntegrationResultReceivedEvent(integrationContext);

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    PROC_URL + WITH_STATUS,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    process.getId(),
                    CloudBPMNActivity.BPMNActivityStatus.COMPLETED
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getStatus, CloudServiceTask::getActivityType)
                    .contains(tuple(CloudBPMNActivity.BPMNActivityStatus.COMPLETED, SERVICE_TASK_TYPE));
            });
    }

    @Test
    public void shouldGetServiceTasks() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    SERVICE_TASKS_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getActivityType)
                    .contains(SERVICE_TASK_TYPE);
            });
    }

    @Test
    void shouldReturn400WhenGetServiceTasksByStatusWithInvalidStatus() {
        ResponseEntity<PagedModel<CloudServiceTask>> running = testRestTemplate.exchange(
            SERVICE_TASKS_URL + WITH_STATUS,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE,
            "RUNNING"
        );
        assertThat(running.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldGetServiceTasksByStatus() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    SERVICE_TASKS_URL + WITH_STATUS,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    CloudBPMNActivity.BPMNActivityStatus.STARTED
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getStatus, CloudServiceTask::getActivityType)
                    .contains(tuple(CloudBPMNActivity.BPMNActivityStatus.STARTED, SERVICE_TASK_TYPE));
            });

        // and given
        sendIntegrationResultReceivedEvent(integrationContext);

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    SERVICE_TASKS_URL + WITH_STATUS,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    CloudBPMNActivity.BPMNActivityStatus.COMPLETED
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody().getContent())
                    .hasSize(1)
                    .extracting(CloudServiceTask::getStatus, CloudServiceTask::getActivityType)
                    .contains(tuple(CloudBPMNActivity.BPMNActivityStatus.COMPLETED, SERVICE_TASK_TYPE));
            });
    }

    @Test
    public void shouldGetServiceTaskById() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        await()
            .untilAsserted(() -> {
                ResponseEntity<PagedModel<CloudServiceTask>> serviceTasksResponse = testRestTemplate.exchange(
                    SERVICE_TASKS_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE
                );

                assertThat(serviceTasksResponse.getBody().getContent()).isNotEmpty();
                String serviceTaskId = serviceTasksResponse.getBody().getContent().iterator().next().getId();

                //when
                ResponseEntity<CloudServiceTask> responseEntity = testRestTemplate.exchange(
                    SERVICE_TASKS_URL + "/" + serviceTaskId,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    SINGLE_TASK_RESPONSE_TYPE
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(responseEntity.getBody()).isNotNull();
                assertThat(responseEntity.getBody())
                    .extracting(
                        CloudServiceTask::getId,
                        CloudServiceTask::getElementId,
                        CloudServiceTask::getActivityType
                    )
                    .containsExactly(serviceTaskId, integrationContext.getId(), SERVICE_TASK_TYPE);
            });
    }

    @Test
    public void shouldGetServiceTaskIntegrationContextErrorById() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        waitForBpmnActivitiesAndSequenceFlows(process.getId());

        ResponseEntity<PagedModel<CloudServiceTask>> serviceTasksResponse = testRestTemplate.exchange(
            SERVICE_TASKS_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );

        assertThat(serviceTasksResponse.getBody().getContent()).isNotEmpty();

        CloudBPMNActivity serviceTask = serviceTasksResponse.getBody().getContent().iterator().next();

        //when
        await()
            .untilAsserted(() -> {
                CloudIntegrationContext cloudIntegrationContext = retrieveIntegrationContext(serviceTask.getId());
                assertThat(cloudIntegrationContext)
                    .extracting(
                        CloudIntegrationContext::getClientId,
                        CloudIntegrationContext::getClientType,
                        CloudIntegrationContext::getRootProcessInstanceId,
                        CloudIntegrationContext::getStatus
                    )
                    .containsExactly(
                        integrationContext.getId(),
                        SERVICE_TASK_TYPE,
                        ROOT_PROCESS_INSTANCE_ID,
                        IntegrationContextStatus.INTEGRATION_REQUESTED
                    );
            });

        // and given
        Throwable cause = new RuntimeException(ERROR_MESSAGE);
        CloudBpmnError error = new CloudBpmnError("ERROR_CODE", cause);

        eventsAggregator.addEvents(
            new CloudIntegrationErrorReceivedEventImpl(
                integrationContext,
                error.getErrorCode(),
                error.getMessage(),
                error.getClass().getName(),
                Arrays.asList(error.getCause().getStackTrace())
            )
        );
        eventsAggregator.sendAll();

        await()
            .untilAsserted(() -> {
                CloudIntegrationContext cloudIntegrationContext = retrieveIntegrationContext(serviceTask.getId());
                assertThat(cloudIntegrationContext)
                    .extracting(
                        CloudIntegrationContext::getClientId,
                        CloudIntegrationContext::getClientType,
                        CloudIntegrationContext::getStatus,
                        CloudIntegrationContext::getErrorCode,
                        CloudIntegrationContext::getErrorMessage,
                        CloudIntegrationContext::getErrorClassName
                    )
                    .containsExactly(
                        integrationContext.getId(),
                        SERVICE_TASK_TYPE,
                        IntegrationContextStatus.INTEGRATION_ERROR_RECEIVED,
                        error.getErrorCode(),
                        StringUtils.truncate(error.getMessage(), ERROR_MESSAGE_LENGTH),
                        error.getClass().getName()
                    );

                assertThat(cloudIntegrationContext.getStackTraceElements()).isNotEmpty();
            });
    }

    @Test
    public void shouldGetServiceTaskIntegrationContextResultById() throws InterruptedException {
        //given
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        CloudServiceTask serviceTask = waitForServiceTask(BPMNActivityStatus.STARTED);

        //when
        await()
            .untilAsserted(() -> {
                CloudIntegrationContext cloudIntegrationContext = retrieveIntegrationContext(serviceTask.getId());
                assertThat(cloudIntegrationContext)
                    .extracting(
                        CloudIntegrationContext::getClientId,
                        CloudIntegrationContext::getClientType,
                        CloudIntegrationContext::getRootProcessInstanceId,
                        CloudIntegrationContext::getStatus
                    )
                    .containsExactly(
                        integrationContext.getId(),
                        SERVICE_TASK_TYPE,
                        ROOT_PROCESS_INSTANCE_ID,
                        IntegrationContextStatus.INTEGRATION_REQUESTED
                    );
            });

        // and given
        sendIntegrationResultReceivedEvent(integrationContext);

        await()
            .untilAsserted(() -> {
                CloudIntegrationContext cloudIntegrationContext = retrieveIntegrationContext(serviceTask.getId());
                assertThat(cloudIntegrationContext)
                    .extracting(
                        CloudIntegrationContext::getClientId,
                        CloudIntegrationContext::getClientType,
                        CloudIntegrationContext::getStatus
                    )
                    .containsExactly(
                        integrationContext.getId(),
                        SERVICE_TASK_TYPE,
                        IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                    );
            });
    }

    @Test
    public void should_supportLoopInvolvingServiceTasks() {
        //given - first iteration, service task started and completed
        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        String id_iteration1 = UUID.randomUUID().toString();
        String id_iteration2 = UUID.randomUUID().toString();

        IntegrationContext integrationContextIt1 = createIntegrationContext(process, id_iteration1);
        CloudServiceTask serviceTaskIt1 = startServiceTask(integrationContextIt1);

        completeServiceTask(integrationContextIt1, serviceTaskIt1, process);

        //when - the process loop back and reaches the task a second time
        IntegrationContext integrationContextIt2 = createIntegrationContext(process, id_iteration2);
        CloudServiceTask serviceTaskIt2 = startServiceTask(integrationContextIt2);

        //then - first iteration service task remains completed
        checkServiceTaskStatus(serviceTaskIt1, BPMNActivityStatus.COMPLETED);

        completeServiceTask(integrationContextIt2, serviceTaskIt2, process);
        checkServiceTaskStatus(serviceTaskIt1, BPMNActivityStatus.COMPLETED);
    }

    private CloudServiceTask startServiceTask(IntegrationContext integrationContext) {
        sendIntegrationRequestedEvent(integrationContext);
        CloudServiceTask serviceTaskIt1 = waitForServiceTask(integrationContext.getId());

        assertThat(serviceTaskIt1.getStatus()).isEqualTo(BPMNActivityStatus.STARTED);
        return serviceTaskIt1;
    }

    private void completeServiceTask(IntegrationContext integrationContext, CloudServiceTask serviceTask, ProcessInstance process) {
        sendIntegrationResultReceivedEvent(integrationContext);
        sendActivityCompletedEvent(serviceTask, process);

        CloudServiceTask serviceTaskIt1 = waitForServiceTask(integrationContext.getId());
        assertThat(serviceTaskIt1.getStatus()).isEqualTo(BPMNActivityStatus.COMPLETED);
        waitForIntegrationContext(serviceTaskIt1, IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
    }

    private void checkServiceTaskStatus(CloudServiceTask serviceTask, BPMNActivityStatus expectedStatus) {
        CloudServiceTask retrievedServiceTask = waitForServiceTask(serviceTask.getId());
        assertThat(retrievedServiceTask.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void shouldSupportBackwardCompatibilityWithOldCompositeKeyForIntegrationResultReceived() {
        //given
        String processInstanceId = UUID.randomUUID().toString();
        String clientId = UUID.randomUUID().toString();
        String executionId = UUID.randomUUID().toString();
        String processDefinitionId = UUID.randomUUID().toString();
        String oldCompositeKey = processInstanceId + ":" + clientId + ":" + executionId;

        persistEntitiesWithOldCompositeKey(oldCompositeKey, processInstanceId, clientId, executionId, processDefinitionId);
        verifyEntitiesPersistedWithOldKey(oldCompositeKey);

        // when: integration result event sent with new ID format (clientId only)
        CloudIntegrationResultReceivedEventImpl event = createIntegrationResultEvent(clientId, processInstanceId, executionId);
        producer.send(event);

        // then: backward compatibility mechanism updates entities with old key
        verifyEntitiesUpdatedWithOldKey(oldCompositeKey);
    }

    private void persistEntitiesWithOldCompositeKey(
        String oldCompositeKey,
        String processInstanceId,
        String clientId,
        String executionId,
        String processDefinitionId
    ) {
        transactionTemplate.execute(status -> {
            ServiceTaskEntity serviceTaskEntity = createServiceTaskEntity(
                oldCompositeKey,
                clientId,
                processInstanceId,
                processDefinitionId,
                executionId
            );
            entityManager.persist(serviceTaskEntity);

            IntegrationContextEntity integrationContextEntity = createIntegrationContextEntity(
                oldCompositeKey,
                clientId,
                processInstanceId,
                processDefinitionId,
                executionId,
                serviceTaskEntity
            );
            entityManager.persist(integrationContextEntity);
            entityManager.flush();

            return null;
        });
    }

    private ServiceTaskEntity createServiceTaskEntity(
        String id,
        String elementId,
        String processInstanceId,
        String processDefinitionId,
        String executionId
    ) {
        ServiceTaskEntity entity = new ServiceTaskEntity(
            "test-service",
            "test-service-full",
            "1.0",
            "test-app",
            "1.0"
        );
        entity.setId(id);
        entity.setElementId(elementId);
        entity.setActivityName(SERVICE_TASK_NAME);
        entity.setActivityType(SERVICE_TASK_TYPE);
        entity.setProcessInstanceId(processInstanceId);
        entity.setProcessDefinitionId(processDefinitionId);
        entity.setExecutionId(executionId);
        entity.setStatus(BPMNActivityStatus.STARTED);
        entity.setStartedDate(new java.util.Date());
        return entity;
    }

    private IntegrationContextEntity createIntegrationContextEntity(
        String id,
        String clientId,
        String processInstanceId,
        String processDefinitionId,
        String executionId,
        ServiceTaskEntity serviceTaskEntity
    ) {
        IntegrationContextEntity entity = new IntegrationContextEntity(
            "test-service",
            "test-service-full",
            "1.0",
            "test-app",
            "1.0"
        );
        entity.setId(id);
        entity.setProcessInstanceId(processInstanceId);
        entity.setRootProcessInstanceId(UUID.randomUUID().toString());
        entity.setExecutionId(executionId);
        entity.setClientId(clientId);
        entity.setClientType(SERVICE_TASK_TYPE);
        entity.setClientName("Service Task");
        entity.setProcessDefinitionId(processDefinitionId);
        entity.setProcessDefinitionKey("testProcess");
        entity.setProcessDefinitionVersion(1);
        entity.setStatus(IntegrationContextStatus.INTEGRATION_REQUESTED);
        entity.setRequestDate(new java.util.Date());
        entity.setServiceTask(serviceTaskEntity);
        return entity;
    }

    private void verifyEntitiesPersistedWithOldKey(String oldCompositeKey) {
        assertThat(integrationContextRepository.findById(oldCompositeKey)).isPresent();
        assertThat(bpmnActivityRepository.findById(oldCompositeKey)).isPresent();
    }

    private CloudIntegrationResultReceivedEventImpl createIntegrationResultEvent(
        String clientId,
        String processInstanceId,
        String executionId
    ) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(clientId);
        integrationContext.setProcessInstanceId(processInstanceId);
        integrationContext.setExecutionId(executionId);
        integrationContext.setClientId(clientId);
        return new CloudIntegrationResultReceivedEventImpl(integrationContext);
    }

    private void verifyEntitiesUpdatedWithOldKey(String oldCompositeKey) {
        await()
            .untilAsserted(() -> {
                IntegrationContextEntity updatedIntegrationContext = integrationContextRepository
                    .findById(oldCompositeKey)
                    .orElseThrow();

                assertThat(updatedIntegrationContext.getStatus())
                    .isEqualTo(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
                assertThat(updatedIntegrationContext.getResultDate()).isNotNull();

                org.activiti.cloud.services.query.model.BPMNActivityEntity updatedServiceTask = bpmnActivityRepository
                    .findById(oldCompositeKey)
                    .orElseThrow();

                assertThat(updatedServiceTask.getStatus()).isEqualTo(BPMNActivityStatus.COMPLETED);
                assertThat(updatedServiceTask.getCompletedDate()).isNotNull();
            });
    }

    @Test
    public void shouldNotGetProcessInstanceServiceTasks() throws InterruptedException {
        //given
        identityTokenProducer.withTestUser("hruser");

        ProcessInstanceImpl process = sendEventsForStartSimpleProcessInstance();
        IntegrationContext integrationContext = createIntegrationContext(process, UUID.randomUUID().toString());
        sendIntegrationRequestedEvent(integrationContext);

        //then
        await()
            .untilAsserted(() -> {
                assertThat(bpmnActivityRepository.findByProcessInstanceId(process.getId())).hasSize(2);
                assertThat(bpmnSequenceFlowRepository.findByProcessInstanceId(process.getId())).hasSize(1);
            });

        await()
            .untilAsserted(() -> {
                //when
                ResponseEntity<PagedModel<CloudServiceTask>> responseEntity = testRestTemplate.exchange(
                    PROC_URL,
                    HttpMethod.GET,
                    identityTokenProducer.entityWithAuthorizationHeader(),
                    PAGED_TASKS_RESPONSE_TYPE,
                    process.getId()
                );
                //then
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            });
    }

    private CloudIntegrationContext retrieveIntegrationContext(String serviceTaskId) {
        ResponseEntity<CloudIntegrationContext> responseEntity = testRestTemplate.exchange(
            SERVICE_TASKS_URL + "/{serviceTaskId}/integration-context",
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            SINGLE_INT_CONTEXT_RESPONSE_TYPE,
            serviceTaskId
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        return responseEntity.getBody();
    }

    private void sendIntegrationResultReceivedEvent(IntegrationContext integrationContext) {
        eventsAggregator.addEvents(new CloudIntegrationResultReceivedEventImpl(integrationContext));

        eventsAggregator.sendAll();
    }

    private void sendIntegrationRequestedEvent(IntegrationContext integrationContext) {
        eventsAggregator.addEvents(new CloudIntegrationRequestedEventImpl(integrationContext));

        eventsAggregator.sendAll();
    }

    private CloudServiceTask waitForServiceTask(BPMNActivityStatus status) {
        await()
            .untilAsserted(() -> {
                final PagedModel<CloudServiceTask> page = testRestTemplate
                    .exchange(
                        SERVICE_TASKS_URL,
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
                        PAGED_TASKS_RESPONSE_TYPE
                    )
                    .getBody();
                assertThat(page).isNotEmpty();
                final CloudServiceTask serviceTask = page.getContent().iterator().next();
                assertThat(serviceTask.getStatus()).isEqualTo(status);
            });
        return retrieveServiceTask();
    }

    private CloudServiceTask waitForServiceTask(String serviceTaskId) {
        await()
            .untilAsserted(() -> {
                final PagedModel<CloudServiceTask> page = testRestTemplate
                    .exchange(
                        SERVICE_TASKS_URL,
                        HttpMethod.GET,
                        identityTokenProducer.entityWithAuthorizationHeader(),
                        PAGED_TASKS_RESPONSE_TYPE
                    )
                    .getBody();
                assertThat(page).isNotEmpty();
                assertThat(page.getContent().stream().filter(task -> serviceTaskId.equals(task.getId())).findFirst())
                    .isPresent();
            });
        return retrieveServiceTask(serviceTaskId);
    }

    private void waitForBpmnActivitiesAndSequenceFlows(String processInstanceId) {
        await()
            .untilAsserted(() -> {
                assertThat(bpmnActivityRepository.findByProcessInstanceId(processInstanceId)).hasSize(2);
                assertThat(bpmnSequenceFlowRepository.findByProcessInstanceId(processInstanceId)).hasSize(1);
            });
    }

    private CloudServiceTask retrieveServiceTask() {
        ResponseEntity<PagedModel<CloudServiceTask>> serviceTasksResponse = testRestTemplate.exchange(
            SERVICE_TASKS_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );

        assertThat(serviceTasksResponse.getBody()).isNotEmpty();

        return serviceTasksResponse.getBody().getContent().iterator().next();
    }

    private CloudServiceTask retrieveServiceTask(String serviceTaskId) {
        ResponseEntity<PagedModel<CloudServiceTask>> serviceTasksResponse = testRestTemplate.exchange(
            SERVICE_TASKS_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            PAGED_TASKS_RESPONSE_TYPE
        );

        assertThat(serviceTasksResponse.getBody()).isNotEmpty();

        return serviceTasksResponse
            .getBody()
            .getContent()
            .stream()
            .filter(task -> serviceTaskId.equals(task.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Service task with ID " + serviceTaskId + " not found"));
    }

    private IntegrationContext createIntegrationContext(ProcessInstanceImpl process, String id) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(id);
        integrationContext.setProcessInstanceId(process.getId());
        integrationContext.setRootProcessInstanceId(ROOT_PROCESS_INSTANCE_ID);
        integrationContext.setExecutionId(EXECUTION_ID);
        integrationContext.setClientId(id);
        integrationContext.setClientType(SERVICE_TASK_TYPE);
        integrationContext.setClientName(SERVICE_TASK_NAME);
        integrationContext.setProcessDefinitionId(process.getProcessDefinitionId());
        integrationContext.setProcessDefinitionVersion(process.getProcessDefinitionVersion());
        integrationContext.setProcessDefinitionKey(process.getProcessDefinitionKey());
        integrationContext.addInBoundVariable("key", "value");
        return integrationContext;
    }

    private void sendActivityCompletedEvent(BPMNActivity bpmnActivity, ProcessInstance processInstance) {
        final CloudBPMNActivityCompletedEventImpl activityCompletedEvent = new CloudBPMNActivityCompletedEventImpl(
            bpmnActivity,
            processInstance.getProcessDefinitionId(),
            processInstance.getId()
        );
        eventsAggregator.addEvents(activityCompletedEvent);
        eventsAggregator.sendAll();
    }

    private void waitForIntegrationContext(CloudServiceTask serviceTask, IntegrationContextStatus status) {
        await()
            .untilAsserted(() -> {
                CloudIntegrationContext cloudIntegrationContext = retrieveIntegrationContext(serviceTask.getId());
                assertThat(cloudIntegrationContext.getStatus()).isEqualTo(status);
            });
    }

    private ProcessInstanceImpl sendEventsForStartSimpleProcessInstance() {
        ProcessInstanceImpl process = startSimpleProcessInstance();
        eventsAggregator.sendAll();
        return process;
    }

    private ProcessInstanceImpl startSimpleProcessInstance() {
        String executionId = UUID.randomUUID().toString();
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setName("process");
        process.setProcessDefinitionKey("mySimpleProcess");
        process.setProcessDefinitionId(processDefinitionId);
        process.setProcessDefinitionVersion(1);

        BPMNActivityImpl startActivity = new BPMNActivityImpl("startEvent1", "", "startEvent");
        startActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        startActivity.setProcessInstanceId(process.getId());
        startActivity.setExecutionId(executionId);

        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl(
            "sid-68945AF1-396F-4B8A-B836-FC318F62313F",
            "startEvent1",
            ELEMENT_ID
        );
        sequenceFlow.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow.setProcessInstanceId(process.getId());

        BPMNActivityImpl activity = buildServiceTask(executionId, process);

        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process, null, null),
            new CloudBPMNActivityStartedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudBPMNActivityCompletedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudSequenceFlowTakenEventImpl(sequenceFlow),
            new CloudBPMNActivityStartedEventImpl(activity, processDefinitionId, process.getId())
        );

        return process;
    }

    private BPMNActivityImpl buildServiceTask(String executionId, ProcessInstanceImpl process) {
        BPMNActivityImpl activity = new BPMNActivityImpl(
            ELEMENT_ID,
            SERVICE_TASK_NAME,
            SERVICE_TASK_TYPE
        );
        activity.setProcessDefinitionId(process.getProcessDefinitionId());
        activity.setProcessInstanceId(process.getId());
        activity.setExecutionId(executionId);
        return activity;
    }
}
