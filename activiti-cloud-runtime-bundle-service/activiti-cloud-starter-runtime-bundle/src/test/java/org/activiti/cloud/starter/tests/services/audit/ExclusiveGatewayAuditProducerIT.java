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

import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_CREATED;
import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_UPDATED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.*;
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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
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

import java.util.*;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ExclusiveGatewayAuditProducerIT {

    private static final String EXCLUSIVE_GATEWAY_PROCESS = "basicExclusiveGateway";
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
    public void testProcessExecutionWithExclusiveGateway() {
        //when
        streamHandler.getAllReceivedEvents().clear();
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(
            new StartProcessPayloadBuilder()
                .withProcessDefinitionKey(EXCLUSIVE_GATEWAY_PROCESS)
                .withProcessDefinitionId(processDefinitionIds.get(EXCLUSIVE_GATEWAY_PROCESS))
                .withVariable("input", 0)
                .build());
        String processInstanceId = processInstance.getBody().getId();
        String processDefinitionKey = processInstance.getBody().getProcessDefinitionKey();

        //then
        Collection<CloudVariableInstance> variableCollection = processInstanceRestTemplate
            .getVariables(processInstance)
            .getBody()
            .getContent();

        assertThat(variableCollection)
            .isNotEmpty()
            .extracting(CloudVariableInstance::getName,
                CloudVariableInstance::getValue)
            .contains(tuple("input", 0));

        //then
        CloudTask task = processInstanceRestTemplate.getTasks(processInstance).getBody().iterator().next();
        String taskId = task.getId();

        await().untilAsserted(() -> {

            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .containsExactly(tuple(PROCESS_CREATED,
                    processInstanceId,
                    processInstanceId),
                    tuple(VARIABLE_CREATED,
                        processInstanceId,
                        "input"),
                    tuple(PROCESS_UPDATED,
                        processInstanceId,
                        processInstanceId),
                    tuple(PROCESS_STARTED,
                        processInstanceId,
                        processInstanceId),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theStart"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theStart"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow1"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "task1"),
                    tuple(VARIABLE_CREATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_CANDIDATE_USER_ADDED,
                        null,
                        "hruser"),
                    tuple(TASK_CREATED,
                        processInstanceId,
                        taskId));

        });

        streamHandler.getAllReceivedEvents().clear();

        //when
        ResponseEntity<CloudTask> claimTask = taskRestTemplate.claim(task);
        assertThat(claimTask).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(TASK_ASSIGNED,
                    processInstanceId,
                    taskId),
                    tuple(TASK_UPDATED,
                        processInstanceId,
                        taskId));

        });

        streamHandler.getAllReceivedEvents().clear();

        //when
        CompleteTaskPayload completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(task.getId())
            .withVariables(Collections.singletonMap("input", 1))
            .build();
        ResponseEntity<CloudTask> completeTask = taskRestTemplate.complete(task, completeTaskPayload);

        //then
        assertThat(completeTask).isNotNull();
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

        variableCollection = processInstanceRestTemplate
            .getVariables(processInstance)
            .getBody()
            .getContent();

        assertThat(variableCollection)
            .isNotEmpty()
            .extracting(CloudVariableInstance::getName,
                CloudVariableInstance::getValue)
            .contains(tuple("input", 1));

        task = processInstanceRestTemplate.getTasks(processInstance).getBody().iterator().next();
        String newTaskId = task.getId();

        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(VARIABLE_UPDATED,
                    processInstanceId,
                    "input"),
                    tuple(VARIABLE_UPDATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_COMPLETED,
                        processInstanceId,
                        taskId),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task1"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow2"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "exclusiveGateway"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "exclusiveGateway"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow3"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "task2"),
                    tuple(VARIABLE_CREATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_CANDIDATE_USER_ADDED,
                        null,
                        "hruser"),
                    tuple(TASK_CREATED,
                        processInstanceId,
                        newTaskId));

            assertThat(receivedEvents)
                .filteredOn(event -> (event.getEventType().equals(ACTIVITY_STARTED) || event.getEventType().equals(ACTIVITY_COMPLETED)) &&
                    ((BPMNActivity) event.getEntity()).getActivityType().equals("exclusiveGateway"))
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessDefinitionKey,
                    event -> ((BPMNActivity) event.getEntity()).getActivityType(),
                    event -> ((BPMNActivity) event.getEntity()).getProcessInstanceId()
                )
                .contains(tuple(ACTIVITY_STARTED,
                    processDefinitionKey,
                    "exclusiveGateway",
                    processInstanceId),
                    tuple(ACTIVITY_COMPLETED,
                        processDefinitionKey,
                        "exclusiveGateway",
                        processInstanceId));

        });

        streamHandler.getAllReceivedEvents().clear();

        //when
        claimTask = taskRestTemplate.claim(task);

        //then
        assertThat(claimTask).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(task.getId())
            .withVariables(Collections.singletonMap("input", 2))
            .build();
        completeTask = taskRestTemplate.complete(task, completeTaskPayload);

        //then
        assertThat(completeTask).isNotNull();
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                .extracting(CloudRuntimeEvent::getEventType,
                    CloudRuntimeEvent::getProcessInstanceId,
                    CloudRuntimeEvent::getEntityId)
                .contains(tuple(TASK_ASSIGNED,
                    processInstanceId,
                    newTaskId),
                    tuple(TASK_UPDATED,
                        processInstanceId,
                        newTaskId),
                    tuple(VARIABLE_UPDATED,
                        processInstanceId,
                        "input"),
                    tuple(VARIABLE_UPDATED,
                        processInstanceId,
                        "input"),
                    tuple(TASK_COMPLETED,
                        processInstanceId,
                        newTaskId),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "task2"),
                    tuple(SEQUENCE_FLOW_TAKEN,
                        processInstanceId,
                        "flow4"),
                    tuple(ACTIVITY_STARTED,
                        processInstanceId,
                        "theEnd1"),
                    tuple(ACTIVITY_COMPLETED,
                        processInstanceId,
                        "theEnd1"),
                    tuple(PROCESS_COMPLETED,
                        processInstanceId,
                        processInstanceId));

            assertThat(receivedEvents)
                .filteredOn(event -> event.getEventType().equals(VARIABLE_UPDATED))
                .extracting(CloudRuntimeEvent::getProcessDefinitionKey,
                    event -> ((VariableInstance) event.getEntity()).getProcessInstanceId(),
                    event -> ((VariableInstance) event.getEntity()).isTaskVariable(),
                    event -> ((VariableInstance) event.getEntity()).getName(),
                    event -> ((VariableInstance) event.getEntity()).getValue())
                .contains(tuple(processDefinitionKey,
                    processInstanceId,
                    true,
                    "input",
                    2),
                    tuple(processDefinitionKey,
                        processInstanceId,
                        false,
                        "input",
                        2));

        });

    }

}
