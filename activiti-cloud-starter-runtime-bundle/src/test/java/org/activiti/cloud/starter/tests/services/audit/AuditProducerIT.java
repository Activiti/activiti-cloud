package org.activiti.cloud.starter.tests.services.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.services.core.model.ProcessDefinition;
import org.activiti.starters.test.MockProcessEngineEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnableBinding(AuditConsumer.class)
public class AuditProducerIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Value("${keycloaktestuser}")
    protected String keycloaktestuser;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static boolean messageRecievedFlag;

    @Before
    public void setUp() throws Exception {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }

    }

    @EnableAutoConfiguration
    public static class StreamHandler {

        @StreamListener(AuditConsumer.AUDIT_CONSUMER)
        public void recieve(MockProcessEngineEvent[] events) {
            assertThat(events).isNotNull();
            assertThat(events.length).isEqualTo(6);
            assertThat(events[0].getEventType()).isEqualTo("ProcessStartedEvent");
            assertThat(events[1].getEventType()).isEqualTo("ActivityStartedEvent");
            assertThat(events[2].getEventType()).isEqualTo("ActivityCompletedEvent");
            assertThat(events[3].getEventType()).isEqualTo("SequenceFlowTakenEvent");
            assertThat(events[4].getEventType()).isEqualTo("ActivityStartedEvent");
            assertThat(events[5].getEventType()).isEqualTo("TaskCreatedEvent");
            messageRecievedFlag = true;
        }
    }

    @Test
    public void shouldRecieveAuditMessage() throws Exception {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        waitForMessage();

        //then
        assertThat(messageRecievedFlag).isTrue();

    }

    private ResponseEntity<PagedResources<ProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<ProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<ProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }

    private void waitForMessage() throws InterruptedException {
        Thread.sleep(500);
    }
}
