package org.activiti.cloud.starter.tests.services.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.runtime.api.event.CloudBPMNActivityStartedEvent;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.Task;
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

import static org.activiti.runtime.api.event.BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED;
import static org.activiti.runtime.api.event.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.runtime.api.event.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED;
import static org.activiti.runtime.api.event.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.runtime.api.event.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
import static org.activiti.runtime.api.event.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED;
import static org.activiti.runtime.api.event.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED;
import static org.activiti.runtime.api.event.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_CANCELLED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_COMPLETED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED;
import static org.activiti.runtime.api.event.VariableEvent.VariableEvents.VARIABLE_CREATED;
import static org.activiti.runtime.api.event.VariableEvent.VariableEvents.VARIABLE_DELETED;
import static org.activiti.runtime.api.event.VariableEvent.VariableEvents.VARIABLE_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditProducerIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    public static final String AUDIT_PRODUCER_IT = "AuditProducerIT";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Before
    public void setUp() {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldProduceEventsDuringSimpleProcessExecution() {
        //when
        ResponseEntity<ProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
                                                                                                                                     Collections.singletonMap("name",
                                                                                                                               "peter"));

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_CREATED.name(),
                                     VARIABLE_CREATED.name(),
                                     PROCESS_STARTED.name(),
                                     ACTIVITY_STARTED.name()/*start event*/,
                                     ACTIVITY_COMPLETED.name()/*start event*/,
                                     SEQUENCE_FLOW_TAKEN.name(),
                                     ACTIVITY_STARTED.name()/*user task*/,
                                     TASK_CANDIDATE_GROUP_ADDED.name(),
                                     TASK_CANDIDATE_USER_ADDED.name(),
                                     TASK_CREATED.name());
            assertThat(receivedEvents)
                    .filteredOn(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                    .extracting(event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityType())
                    .containsExactly("startEvent",
                                     "userTask");
        });

        //when
        processInstanceRestTemplate.suspend(startProcessEntity);

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_SUSPENDED.name(),
                                 TASK_SUSPENDED.name()));

        //when
        processInstanceRestTemplate.resume(startProcessEntity);

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_RESUMED.name(),
                                 TASK_ACTIVATED.name()));

        //when
        processInstanceRestTemplate.setVariables(startProcessEntity.getBody().getId(),
                                                 Collections.singletonMap("name",
                                                                          "paul"));

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(VARIABLE_UPDATED.name()));

        //given
        ResponseEntity<PagedResources<Task>> tasks = processInstanceRestTemplate.getTasks(startProcessEntity);
        Task task = tasks.getBody().iterator().next();

        //when
        taskRestTemplate.claim(task);
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(TASK_ASSIGNED.name()));

        //when
        taskRestTemplate.complete(task);

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(TASK_COMPLETED.name(),
                                 TASK_CANDIDATE_GROUP_REMOVED.name(),
                                 TASK_CANDIDATE_USER_REMOVED.name(),
                                 ACTIVITY_COMPLETED.name()/*user task*/,
                                 SEQUENCE_FLOW_TAKEN.name(),
                                 ACTIVITY_STARTED.name()/*end event*/,
                                 ACTIVITY_COMPLETED.name()/*end event*/,
                                 VARIABLE_DELETED.name(),
                                 PROCESS_COMPLETED.name()));
    }

    @Test
    public void shouldProduceEventsForAProcessDeletion() {
        //given
        ResponseEntity<ProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        processInstanceRestTemplate.delete(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getReceivedEvents();
            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(TASK_CANCELLED.name(),
                                     ACTIVITY_CANCELLED.name(),
                                     TASK_CANDIDATE_GROUP_REMOVED.name(),
                                     TASK_CANDIDATE_USER_REMOVED.name(),
                                     PROCESS_CANCELLED.name());
        });
    }

    private ResponseEntity<PagedResources<ProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<ProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<ProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }
}
