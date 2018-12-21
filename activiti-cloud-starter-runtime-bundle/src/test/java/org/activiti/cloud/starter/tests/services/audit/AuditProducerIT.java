package org.activiti.cloud.starter.tests.services.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_CREATED;
import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_UPDATED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED;
import static org.activiti.api.process.model.events.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED;
import static org.activiti.api.task.model.events.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED;
import static org.activiti.api.task.model.events.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CANCELLED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_COMPLETED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditProducerIT {

    public static final String ROUTING_KEY_HEADER = "routingKey";
    public static final String[] RUNTIME_BUNDLE_INFO_HEADERS = {"appName", "appVersion", "serviceName", "serviceVersion", "serviceFullName", ROUTING_KEY_HEADER};
    public static final String[] EXECUTION_CONTEXT_HEADERS = {"businessKey", "processDefinitionId", "processDefinitionKey", "processInstanceId",
                                                              "processName", "deploymentId", "deploymentName"};
    public static final String[] ALL_REQUIRED_HEADERS = Stream.of(RUNTIME_BUNDLE_INFO_HEADERS, EXECUTION_CONTEXT_HEADERS)
                                                     .flatMap(Stream::of)
                                                     .toArray(String[]::new);

    public static final String AUDIT_PRODUCER_IT = "AuditProducerIT";
    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;
    @Autowired
    private TaskRestTemplate taskRestTemplate;
    @Autowired
    private AuditConsumerStreamHandler streamHandler;
    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Before
    public void setUp() {
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody()).isNotNull();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (CloudProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                    pd.getId());
        }
    }

    @Test
    public void shouldProduceEventsForProcessDeployment() {
        //when
        List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

        assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

        //then
        assertThat(receivedEvents)
                .filteredOn(event -> ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name().equals(event.getEventType().name()))
                .extracting(event -> ((ProcessDefinition)event.getEntity()).getKey())
                .contains(SIMPLE_PROCESS);
    }

    @Test
    public void shouldProduceEventsDuringSimpleProcessExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(ProcessPayloadBuilder
                                                                                                                   .start()
                                                                                                                   .withProcessDefinitionKey(SIMPLE_PROCESS)
                                                                                                                   .withVariable("name",
                                                                                                                                 "peter")
                                                                                                                   .withProcessInstanceName("my instance name")
                .withBusinessKey("businessKey")
                                                                                                                   .build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            
            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_CREATED.name(),
                                     VARIABLE_CREATED.name(),
                                     PROCESS_STARTED.name(),
                                     ACTIVITY_STARTED.name()/*start event*/,
                                     BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*start event*/,
                                     SEQUENCE_FLOW_TAKEN.name(),
                                     ACTIVITY_STARTED.name()/*user task*/,
                                     VARIABLE_CREATED.name(), /*task variable copy of proc var*/
                                     TASK_CANDIDATE_GROUP_ADDED.name(),
                                     TASK_CANDIDATE_USER_ADDED.name(),
                                     TASK_CREATED.name());
            assertThat(receivedEvents)
                    .filteredOn(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                    .extracting(event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityType())
                    .containsExactly("startEvent",
                            "userTask");
            assertThat(receivedEvents).filteredOn(cloudRuntimeEvent -> PROCESS_STARTED.equals(cloudRuntimeEvent.getEventType()))
                    .extracting(cloudRuntimeEvent -> ((ProcessInstance)cloudRuntimeEvent.getEntity()).getName())
                    .containsExactly("my instance name");
        });

        //when
        processInstanceRestTemplate.suspend(startProcessEntity);

        //then
        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            
            assertThat(streamHandler.getLatestReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_SUSPENDED.name(),
                        TASK_SUSPENDED.name());
        });

        //when
        processInstanceRestTemplate.resume(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            
            assertThat(streamHandler.getLatestReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_RESUMED.name(),
                        TASK_ACTIVATED.name());
        });

        //when
        processInstanceRestTemplate.setVariables(startProcessEntity.getBody().getId(),
                Collections.singletonMap("name",
                        "paul"));

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            
            assertThat(streamHandler.getLatestReceivedEvents())
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(VARIABLE_UPDATED.name());
        });

        //given
        ResponseEntity<PagedResources<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startProcessEntity);
        Task task = tasks.getBody().iterator().next();

        //when
        taskRestTemplate.claim(task);
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
                            
            assertThat(streamHandler.getLatestReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(TASK_ASSIGNED.name(),
                                 TASK_UPDATED.name()
                );
        });

        //when
        taskRestTemplate.complete(task);

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
                            
            assertThat(streamHandler.getLatestReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(VARIABLE_UPDATED.name(),/*task local var copied back to proc var*/
                                 TASK_COMPLETED.name(),
                                 TASK_CANDIDATE_GROUP_REMOVED.name(),
                                 TASK_CANDIDATE_USER_REMOVED.name(),
                                 BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*user task*/,
                                 SEQUENCE_FLOW_TAKEN.name(),
                                 ACTIVITY_STARTED.name()/*end event*/,
                                 BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*end event*/,
                                 PROCESS_COMPLETED.name());
        });

            assertThat(streamHandler.getLatestReceivedEvents())
                    .filteredOn(event -> event.getEventType().equals(TASK_COMPLETED))
                    .extracting(event -> ((Task) event.getEntity()).getStatus())
                    .containsOnly(Task.TaskStatus.COMPLETED);
    }

    @Test
    public void shouldProduceEventsForAProcessDeletion() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
                                                                                                               .withProcessDefinitionId(processDefinitionIds.get(SIMPLE_PROCESS))
                                                                                                               .withProcessInstanceName("processInstanceName")
                                                                                                               .withBusinessKey("businessKey")
                                                                                                               .withVariables(Collections.emptyMap())
                                                                                                               .build());

        //when
        processInstanceRestTemplate.delete(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
            
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(TASK_CANCELLED.name(),
                            ACTIVITY_CANCELLED.name(),
                            TASK_CANDIDATE_GROUP_REMOVED.name(),
                            TASK_CANDIDATE_USER_REMOVED.name(),
                            PROCESS_CANCELLED.name());
        });
    }

    @Test
    public void shouldProduceEventsForAProcessUpdate() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        processInstanceRestTemplate.update(startProcessEntity,
                                          "businessKey",
                                          "name");



        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_UPDATED.name());

            assertThat(receivedEvents)
                    .extracting(event -> event.getEntity())
                    .extracting(ProcessInstance.class::cast)
                    .extracting(event -> event.getName())
                    .containsExactly("name");

            assertThat(receivedEvents)
                    .extracting(event -> event.getEntity())
                    .extracting(ProcessInstance.class::cast)
                    .extracting(entity -> entity.getBusinessKey())
                    .containsExactly("businessKey");
        });
    }

    @Test
    public void shouldEmitEventsForTaskDelete() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("my task name").withDescription(
                "long description here").build());

        //when
        ResponseEntity<CloudTask> deleteTask = taskRestTemplate.delete(task);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .hasSize(2)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_CANDIDATE_USER_REMOVED,
                                           "hruser"),
                            tuple(TASK_CANCELLED,
                                    task.getId())
                    );

            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(TaskCandidateUser.class);
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getUserId()).isEqualTo("hruser");
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getTaskId()).isEqualTo(task.getId());
            assertThat(receivedEvents.get(0).getEntityId()).isEqualTo("hruser");
            assertThat(receivedEvents.get(1).getEntity()).isNotNull();
            assertThat(receivedEvents.get(1).getEntity()).isInstanceOf(Task.class);
            assertThat(((Task) receivedEvents.get(1).getEntity()).getStatus()).isEqualTo(Task.TaskStatus.CANCELLED);
            assertThat(((Task) receivedEvents.get(1).getEntity()).getId()).isEqualTo(task.getId());
            assertThat(receivedEvents.get(1).getEntityId()).isEqualTo(task.getId());
        });
    }

    private void assertThatHeadersExists(Map<String, Object> headers) {
        assertThat(headers)
            .containsKeys("businessKey",
                          "processDefinitionId",
                          "processDefinitionKey",
                          "deploymentId",
                          "deploymentName",
                          "appName",
                          "appVersion",
                          "serviceName",
                          "serviceVersion",
                          "serviceFullName");
    }


    @Test
    public void shouldEmitEventsForTaskUpdate() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("my task name 2").withDescription(
                "long description here").withAssignee("hruser").build());

        //when
        taskRestTemplate.updateTask(TaskPayloadBuilder.update().withTaskId(task.getId()).withDescription("short description").build());


        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_UPDATED,
                                    task.getId())
                    );


            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(Task.class);
            assertThat(((Task) receivedEvents.get(0).getEntity()).getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
            assertThat(((Task) receivedEvents.get(0).getEntity()).getId()).isEqualTo(task.getId());
            assertThat(receivedEvents.get(0).getEntityId()).isEqualTo(task.getId());
            assertThat(((Task) receivedEvents.get(0).getEntity()).getDescription()).isEqualTo("short description");
        });
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                HttpMethod.GET,
                null,
                responseType);
    }
}
