/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskAssignmentEventReceiptRepository;
import org.activiti.cloud.services.query.app.repository.TaskAssignmentHistoryRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskAssignmentHistoryEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starters.test.EventsAggregator;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.builder.ProcessInstanceEventContainedBuilder;
import org.activiti.cloud.starters.test.builder.TaskEventContainedBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = KeycloakContainerApplicationInitializer.class)
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
class QueryTaskAssignmentHistoryIT {

    private static final ParameterizedTypeReference<List<TaskAssignmentHistoryEntity>> HISTORY_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    private static final String HISTORY_URL = "/admin/v1/tasks/{taskId}/assignment-history";

    @Autowired
    private MyProducer producer;

    @Autowired
    private QueryConsumerChannelHandler queryConsumer;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskAssignmentHistoryRepository historyRepository;

    @Autowired
    private TaskAssignmentEventReceiptRepository receiptRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @BeforeEach
    void setUp() {
        identityTokenProducer.withTestUser("testadmin");
    }

    @AfterEach
    void tearDown() {
        historyRepository.deleteAll();
        receiptRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    void shouldPersistAssignmentEventsAndReturnHistoryNewestFirst() {
        String taskId = createTask(null);
        String otherTaskId = createTask(null);
        CloudTaskAssignedEventImpl first = assignment("a", 100L, taskId, "user-1", "ASSIGNED", "Initial assignment");
        CloudTaskAssignedEventImpl unchanged = assignment("noop", 150L, taskId, "user-1", "ASSIGNED", "No change");
        CloudTaskAssignedEventImpl claimed = assignment("b", 200L, taskId, "user-2", "CLAIMED", "Workload");
        CloudTaskAssignedEventImpl released = assignment("c", 200L, taskId, null, "RELEASED", null);
        producer.send(first, unchanged, claimed, released, assignment("other", 300L, otherTaskId, "other", null, null));

        await().untilAsserted(() -> {
            ResponseEntity<List<TaskAssignmentHistoryEntity>> response = history(taskId);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                .extracting(
                    TaskAssignmentHistoryEntity::getId,
                    TaskAssignmentHistoryEntity::getTimestamp,
                    TaskAssignmentHistoryEntity::getPreviousAssignee,
                    TaskAssignmentHistoryEntity::getNewAssignee,
                    TaskAssignmentHistoryEntity::getAction,
                    TaskAssignmentHistoryEntity::getReason
                )
                .containsExactly(
                    tuple("c", 200L, "user-2", null, "RELEASED", null),
                    tuple("b", 200L, "user-1", "user-2", "CLAIMED", "Workload"),
                    tuple("a", 100L, null, "user-1", "ASSIGNED", "Initial assignment")
                );
            assertThat(response.getBody()).extracting(TaskAssignmentHistoryEntity::getActor).containsOnly("admin");
            assertThat(response.getBody()).extracting(TaskAssignmentHistoryEntity::getTaskId).containsOnly(taskId);
        });

        queryConsumer.receive(List.of(first, unchanged, claimed, released), Map.of());
        assertThat(historyRepository.findByTaskIdOrderByTimestampDescIdDesc(taskId)).hasSize(3);
        assertThat(taskRepository.findById(taskId).orElseThrow().getAssignee()).isNull();
    }

    @Test
    void shouldCaptureInitialAutomaticAssignmentOnlyOnce() {
        String taskId = createTask("automatic-user");
        CloudTaskAssignedEventImpl initial = assignment("initial-assigned", 2L, taskId, "automatic-user", null, null);
        producer.send(initial);

        await().untilAsserted(() -> {
            assertThat(receiptRepository.existsById(initial.getId())).isTrue();
            List<TaskAssignmentHistoryEntity> entries = history(taskId).getBody();
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst())
                .returns(null, TaskAssignmentHistoryEntity::getPreviousAssignee)
                .returns("automatic-user", TaskAssignmentHistoryEntity::getNewAssignee)
                .returns("ASSIGNED", TaskAssignmentHistoryEntity::getAction)
                .returns("service_user", TaskAssignmentHistoryEntity::getActor)
                .returns(null, TaskAssignmentHistoryEntity::getReason);
        });
        queryConsumer.receive(List.of(assignment("later", 3L, taskId, "next-user", null, null)), Map.of());
        queryConsumer.receive(List.of(initial), Map.of());
        assertThat(history(taskId).getBody()).hasSize(2);
        assertThat(taskRepository.findById(taskId).orElseThrow().getAssignee()).isEqualTo("next-user");
    }

    @Test
    void shouldAcceptAssignmentEventsWithoutNewMetadata() {
        String taskId = createTask(null);
        producer.send(
            assignment("legacy-assign", 100L, taskId, "user", null, null),
            assignment("legacy-release", 200L, taskId, null, null, null)
        );

        await().untilAsserted(() -> {
            assertThat(history(taskId).getBody())
                .extracting(TaskAssignmentHistoryEntity::getAction, TaskAssignmentHistoryEntity::getReason)
                .containsExactly(tuple("RELEASED", null), tuple("ASSIGNED", null));
        });
    }

    @Test
    void shouldReturnEmptyHistoryForUnassignedTaskAndNotFoundForUnknownTask() {
        assertThat(history(createTask(null)).getBody()).isEmpty();
        assertThat(historyStatus("missing-task")).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectNonAdminEvenWhenAssignedToTheTask() {
        String taskId = createTask("testuser");
        identityTokenProducer.withTestUser("testuser");

        assertThat(historyStatus(taskId)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRollBackHistoryWithTheTaskProjectionWhenAnEventFails() {
        String taskId = createTask(null);

        assertThatThrownBy(() ->
            queryConsumer.receive(
                List.of(
                    assignment("before-failure", 100L, taskId, "user", null, null),
                    assignment("failure", 200L, "missing-task", "user", null, null)
                ),
                Map.of()
            )
        ).isInstanceOf(QueryException.class);

        assertThat(historyRepository.findByTaskIdOrderByTimestampDescIdDesc(taskId)).isEmpty();
        assertThat(receiptRepository.existsById("before-failure")).isFalse();
        assertThat(taskRepository.findById(taskId).orElseThrow().getAssignee()).isNull();
    }

    @Test
    void shouldDeleteHistoryWhenDeletingItsTask() {
        String taskId = createTask("automatic-user");
        queryConsumer.receive(List.of(assignment("delete-receipt", 2L, taskId, "next-user", null, null)), Map.of());

        ResponseEntity<Void> response = restTemplate.exchange(
            "/admin/v1/tasks?id={taskId}",
            HttpMethod.DELETE,
            identityTokenProducer.entityWithAuthorizationHeader(),
            Void.class,
            taskId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyRepository.findByTaskIdOrderByTimestampDescIdDesc(taskId)).isEmpty();
        assertThat(receiptRepository.findByTaskIdIn(List.of(taskId))).isEmpty();
        assertThat(historyStatus(taskId)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteHistoryAndReceiptsWhenDeletingItsProcessViaRest() {
        EventsAggregator events = new EventsAggregator(producer);
        ProcessInstance process = new ProcessInstanceEventContainedBuilder(events).aRunningProcessInstance(
            "history-process"
        );
        Task task = new TaskEventContainedBuilder(events).anAssignedTask("history-task", "user", process);
        events.sendAll();
        await().untilAsserted(() -> assertThat(receiptRepository.findByTaskIdIn(List.of(task.getId()))).hasSize(1));

        ResponseEntity<Void> response = restTemplate.exchange(
            "/admin/v1/process-instances?id={processInstanceId}",
            HttpMethod.DELETE,
            identityTokenProducer.entityWithAuthorizationHeader(),
            Void.class,
            process.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyRepository.findByTaskIdOrderByTimestampDescIdDesc(task.getId())).isEmpty();
        assertThat(receiptRepository.findByTaskIdIn(List.of(task.getId()))).isEmpty();
        assertThat(taskRepository.existsById(task.getId())).isFalse();
    }

    @Test
    void shouldDeleteHistoryAndReceiptsWhenReceivingAProcessDeletedEvent() {
        EventsAggregator events = new EventsAggregator(producer);
        ProcessInstance process = new ProcessInstanceEventContainedBuilder(events).aRunningProcessInstance(
            "history-process"
        );
        Task task = new TaskEventContainedBuilder(events).anAssignedTask("history-task", "user", process);
        events.sendAll();
        await().untilAsserted(() -> assertThat(receiptRepository.findByTaskIdIn(List.of(task.getId()))).hasSize(1));

        producer.send(new CloudProcessCompletedEventImpl(process), new CloudProcessDeletedEventImpl(process));

        await().untilAsserted(() -> {
            assertThat(taskRepository.existsById(task.getId())).isFalse();
            assertThat(historyRepository.findByTaskIdOrderByTimestampDescIdDesc(task.getId())).isEmpty();
            assertThat(receiptRepository.findByTaskIdIn(List.of(task.getId()))).isEmpty();
        });
    }

    private String createTask(String assignee) {
        String id = UUID.randomUUID().toString();
        TaskImpl task = task(id, assignee);
        producer.send(new CloudTaskCreatedEventImpl("created-" + id, 1L, task));
        await().untilAsserted(() -> assertThat(taskRepository.findById(id)).isPresent());
        return id;
    }

    private static TaskImpl task(String taskId, String assignee) {
        TaskImpl task = new TaskImpl(
            taskId,
            "Assignment history task",
            assignee == null ? Task.TaskStatus.CREATED : Task.TaskStatus.ASSIGNED
        );
        task.setAssignee(assignee);
        return task;
    }

    private static CloudTaskAssignedEventImpl assignment(
        String id,
        Long timestamp,
        String taskId,
        String assignee,
        String action,
        String reason
    ) {
        CloudTaskAssignedEventImpl event = new CloudTaskAssignedEventImpl(id, timestamp, task(taskId, assignee));
        event.setActor("admin");
        event.setAssignmentAction(action);
        event.setReason(reason);
        return event;
    }

    private ResponseEntity<List<TaskAssignmentHistoryEntity>> history(String taskId) {
        return restTemplate.exchange(
            HISTORY_URL,
            HttpMethod.GET,
            identityTokenProducer.entityWithAuthorizationHeader(),
            HISTORY_RESPONSE_TYPE,
            taskId
        );
    }

    private HttpStatusCode historyStatus(String taskId) {
        return restTemplate
            .exchange(
                HISTORY_URL,
                HttpMethod.GET,
                identityTokenProducer.entityWithAuthorizationHeader(),
                Void.class,
                taskId
            )
            .getStatusCode();
    }
}
