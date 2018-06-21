package org.activiti.cloud.starter.tests.services.audit;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
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

import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.activiti.runtime.api.event.ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED;
import static org.activiti.runtime.api.event.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
import static org.activiti.runtime.api.event.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED;
import static org.activiti.runtime.api.event.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
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
    public void shouldReceiveAuditMessage() {
        //when
        ResponseEntity<ProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_CREATED.name(),
                          PROCESS_STARTED.name(),
                          TASK_CANDIDATE_GROUP_ADDED.name(),
                          TASK_CANDIDATE_USER_ADDED.name(),
                          TASK_CREATED.name()));

        //when
        processInstanceRestTemplate.suspend(startProcessEntity);

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_SUSPENDED.name()));

        //when
        processInstanceRestTemplate.resume(startProcessEntity);

        //then
        await().untilAsserted(() -> assertThat(streamHandler.getReceivedEvents())
                .extracting(event -> event.getEventType().name())
                .containsExactly(PROCESS_RESUMED.name()));



        //todo add events for:
        // - completed process
        // - rolled back
        // - AsyncErrorProcess
        // - Task created
        // - process deleted
        // - task deleted
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
