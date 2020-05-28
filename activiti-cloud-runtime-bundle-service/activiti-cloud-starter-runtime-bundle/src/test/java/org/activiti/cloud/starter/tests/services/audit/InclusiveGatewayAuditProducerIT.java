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
package org.activiti.cloud.starter.tests.services.audit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_CREATED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.api.process.model.events.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.api.task.model.events.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_COMPLETED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_UPDATED;
import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.ALL_REQUIRED_HEADERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class InclusiveGatewayAuditProducerIT {

    private static final String INCLUSIVE_GATEWAY_PROCESS = "basicInclusiveGateway";

    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Autowired
    private TestRestTemplate restTemplate;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;


    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;


    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody()).isNotNull();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (CloudProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getKey(), pd.getId());
        }

    }

    private ResponseEntity<PagedModel<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
            HttpMethod.GET,
            null,
            responseType);
    }

    @Test
    public void testProcessExecutionWithInclusiveGateway() {
        //when
        streamHandler.getAllReceivedEvents().clear();
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            new StartProcessPayloadBuilder()
                .withProcessDefinitionKey(INCLUSIVE_GATEWAY_PROCESS)
                .withProcessDefinitionId(processDefinitionIds.get(INCLUSIVE_GATEWAY_PROCESS))
                .withVariable("input", 1)
                .build());
        String processInstanceId = processInstance.getBody().getId();

        //then task0 is started
        CloudTask task = processInstanceRestTemplate.getTasks(processInstance).getBody().iterator().next();
        String taskId = task.getId();

        streamHandler.getAllReceivedEvents().clear();

        //when
        ResponseEntity<CloudTask> claimTask = taskRestTemplate.claim(task);
        assertThat(claimTask).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        streamHandler.getAllReceivedEvents().clear();

        //when
        CompleteTaskPayload completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(task.getId())
            .build();
        ResponseEntity<CloudTask> completeTask = taskRestTemplate.complete(task, completeTaskPayload);

        //then
        assertThat(completeTask).isNotNull();
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

        //then - two tasks should be available
        Iterator<CloudTask> tasks = processInstanceRestTemplate.getTasks(processInstance).getBody().getContent().iterator();

        CloudTask task1 = tasks.hasNext() ? tasks.next() : null;
        CloudTask task2 = tasks.hasNext() ? tasks.next() : null;

        assertThat(task1).isNotNull();
        assertThat(task2).isNotNull();

        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(TASK_COMPLETED,
                    processInstanceId,
                    taskId),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task0"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow2"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "inclusiveGateway"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "inclusiveGateway"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow3"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theTask1"),
                    tuple(VARIABLE_CREATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_CANDIDATE_USER_ADDED,
                        null,
                        "hruser"),
                    tuple(TASK_CREATED,
                        processInstanceId,
                        task1.getId()),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow4"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theTask2"),
                    tuple(VARIABLE_CREATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_CANDIDATE_USER_ADDED,
                        null,
                        "hruser"),
                    tuple(TASK_CREATED,
                        processInstanceId,
                        task2.getId()));

        });

        streamHandler.getAllReceivedEvents().clear();

        //when - complete first task
        claimTask = taskRestTemplate.claim(task1);
        assertThat(claimTask).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(task.getId())
            .build();
        completeTask = taskRestTemplate.complete(task1, completeTaskPayload);
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

        //then - first task should be completed, second should be available
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(TASK_ASSIGNED,
                    processInstanceId,
                    task1.getId()),
                    tuple(TASK_UPDATED,
                        processInstanceId,
                        task1.getId()),
                    tuple(TASK_COMPLETED,
                        processInstanceId,
                        task1.getId()),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theTask1"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow6"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "inclusiveGatewayEnd"));

        });

        tasks = processInstanceRestTemplate.getTasks(processInstance).getBody().getContent().iterator();
        assertThat(tasks).toIterable().hasSize(1);

        streamHandler.getAllReceivedEvents().clear();

        //when - complete second task
        claimTask = taskRestTemplate.claim(task2);
        assertThat(claimTask).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(task2.getId())
            .build();
        completeTask = taskRestTemplate.complete(task2, completeTaskPayload);
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

        //then - second task should be completed, process should be completed
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(TASK_ASSIGNED,
                    processInstanceId,
                    task2.getId()),
                    tuple(TASK_UPDATED,
                        processInstanceId,
                        task2.getId()),
                    tuple(TASK_COMPLETED,
                        processInstanceId,
                        task2.getId()),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theTask2"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow7"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "inclusiveGatewayEnd"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "inclusiveGatewayEnd"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow9"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theEnd"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theEnd"),
                    tuple(PROCESS_COMPLETED,
                        processInstanceId,
                        processInstanceId));

        });

    }

}
