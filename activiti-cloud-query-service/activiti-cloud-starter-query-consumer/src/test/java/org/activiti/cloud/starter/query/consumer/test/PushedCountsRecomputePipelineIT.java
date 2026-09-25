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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.app.AssignedTaskCounter;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.app.QueryConsumerMessageHandler;
import org.activiti.cloud.services.query.app.RunningProcessesCounter;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * End-to-end: a real committed event batch, captured after commit, flushed by the real scheduler,
 * resolved against a directly-seeded {@link ConsumerSubscriberRegistry}, counted by a fixed-result
 * {@link AssignedTaskCounter} test double, and published onto the real {@code countProducer} binding.
 */
@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "activiti.cloud.services.oauth2.iam-name=test",
        "activiti.cloud.query.pushed-counts.enabled=true",
        "activiti.features.query.pushed-counts.enabled=true",
        "activiti.cloud.query.pushed-counts.flush-interval=PT0.1S",
        "activiti.cloud.query.pushed-counts.flush-max-window=PT0.2S",
        "activiti.cloud.query.pushed-counts.buffer-hard-cap=1",
    }
)
@EnableTestBinder
@Import(PushedCountsRecomputePipelineIT.TestCounterConfig.class)
class PushedCountsRecomputePipelineIT {

    private static final String COUNT_DESTINATION = "pushedCounts";

    @Autowired
    private QueryConsumerMessageHandler messageHandler;

    @Autowired
    private ConsumerSubscriberRegistry registry;

    @Autowired
    private OutputDestination output;

    @AfterEach
    void tearDown() {
        // Tests share one context; drop registrations so the next test starts with no watchers.
        registry.unregister("alice", "rest-1", Instant.now());
        registry.unregister("frank", "rest-1", Instant.now());
        registry.unregister("grace", "rest-1", Instant.now());
        registry.unregister("henry", "rest-1", Instant.now());
    }

    @Test
    void committedTaskEvents_flowThroughToACountChangedMessage_forAWatchingUser() {
        registry.register("alice", Set.of(), "rest-1", Instant.now());

        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        List<CloudRuntimeEvent<?, ?>> events = List.of(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl("evt-1", System.currentTimeMillis(), assignedTask())
        );
        Message<List<CloudRuntimeEvent<?, ?>>> message = MessageBuilder.withPayload(events).build();

        messageHandler.accept(message);

        Message<byte[]> assignedMessage = receiveContaining(output, "assigned:alice", 5000);
        assertThat(assignedMessage).isNotNull();
        assertThat(new String(assignedMessage.getPayload(), StandardCharsets.UTF_8))
            .contains("assigned:alice")
            .contains("\"count\":5");
        assertThat(receiveContaining(output, "processes:alice", 2000)).isNotNull();
    }

    @Test
    void committedTaskEvents_forAnUnwatchedUser_publishNothing() {
        TaskImpl task = new TaskImpl();
        task.setId("task-2");
        List<CloudRuntimeEvent<?, ?>> events = List.of(new CloudTaskCreatedEventImpl(task));
        Message<List<CloudRuntimeEvent<?, ?>>> message = MessageBuilder.withPayload(events).build();

        messageHandler.accept(message);

        assertThat(output.receive(1000, COUNT_DESTINATION)).isNull();
    }

    @Test
    void touchingMoreTasksThanTheHardCap_dropsTheOverflow_soOnlyTheFirstIsCounted() {
        // buffer-hard-cap=1 above; the drop happens at capture time, so this test doesn't need
        // its own context - it can share this class's already-booted one.
        registry.register("frank", Set.of(), "rest-1", Instant.now());
        registry.register("grace", Set.of(), "rest-1", Instant.now());

        List<CloudRuntimeEvent<?, ?>> events = List.of(
            new CloudTaskCreatedEventImpl(assignedTask("task-3", "frank")),
            new CloudTaskCreatedEventImpl(assignedTask("task-4", "grace"))
        );
        messageHandler.accept(MessageBuilder.withPayload(events).build());

        Message<byte[]> received = receiveContaining(output, "assigned:frank", 5000);
        assertThat(received).isNotNull();
        assertThat(new String(received.getPayload(), StandardCharsets.UTF_8)).contains("assigned:frank");
        assertThat(receiveContaining(output, "processes:frank", 2000)).isNotNull();
        assertThat(receiveContaining(output, "grace", 500)).isNull();
    }

    @Test
    void committedProcessStartedEvent_flowsThroughToACountChangedMessage_forAWatchingInitiator() {
        registry.register("henry", Set.of(), "rest-1", Instant.now());

        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("proc-1");
        process.setInitiator("henry");
        // ProcessStartedEventHandler requires the row to already exist.
        List<CloudRuntimeEvent<?, ?>> events = List.of(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process)
        );
        messageHandler.accept(MessageBuilder.withPayload(events).build());

        Message<byte[]> received = receiveContaining(output, "processes:henry", 5000);
        assertThat(received).isNotNull();
        String payload = new String(received.getPayload(), StandardCharsets.UTF_8);
        assertThat(payload).contains("processes:henry").contains("\"count\":7");
    }

    private static TaskImpl assignedTask() {
        TaskImpl task = new TaskImpl();
        task.setId("task-1");
        task.setAssignee("alice");
        return task;
    }

    @TestConfiguration
    static class TestCounterConfig {

        @Bean
        AssignedTaskCounter testAssignedCounter(TaskRepository taskRepository) {
            // Replaces the real ASSIGNED counter (via @ConditionalOnMissingBean) so the pipeline runs exactly one, with a fixed count.
            return new AssignedTaskCounter(taskRepository) {
                @Override
                public Map<String, Long> compute(Set<String> affectedUserIds) {
                    return Map.of("alice", 5L);
                }
            };
        }

        @Bean
        RunningProcessesCounter testRunningProcessesCounter(
            ProcessInstanceRepository processInstanceRepository,
            TaskRepository taskRepository,
            TaskCandidateUserRepository taskCandidateUserRepository
        ) {
            return new RunningProcessesCounter(processInstanceRepository, taskRepository, taskCandidateUserRepository) {
                @Override
                public Map<String, Long> compute(Set<String> affectedUserIds) {
                    return Map.of("henry", 7L);
                }
            };
        }
    }

    /**
     * A long {@code flush-max-window} (10s) with a tiny {@code flush-max-size} (2), so a flush
     * arriving within a few hundred ms can only be explained by the size trigger, not the window.
     * A separate nested context (own properties), independent of the outer class's.
     */
    @Nested
    @SpringBootTest(
        classes = QueryConsumerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "activiti.cloud.services.oauth2.iam-name=test",
            "activiti.cloud.query.pushed-counts.enabled=true",
            "activiti.features.query.pushed-counts.enabled=true",
            "activiti.cloud.query.pushed-counts.flush-interval=PT0.1S",
            "activiti.cloud.query.pushed-counts.flush-max-window=PT10S",
            "activiti.cloud.query.pushed-counts.flush-max-size=2",
            "activiti.cloud.query.pushed-counts.buffer-hard-cap=100",
        }
    )
    @EnableTestBinder
    @Import(PushedCountsRecomputePipelineIT.TestCounterConfig.class)
    class SoftCapScenario {

        @Autowired
        private QueryConsumerMessageHandler messageHandler;

        @Autowired
        private ConsumerSubscriberRegistry registry;

        @Autowired
        private OutputDestination output;

        @AfterEach
        void tearDown() {
            registry.unregister("erin", "rest-1", Instant.now());
        }

        @Test
        void touchingAsManyTasksAsTheSoftCap_flushesEarly_withoutWaitingOutTheMuchLongerWindow() {
            registry.register("erin", Set.of(), "rest-1", Instant.now());

            List<CloudRuntimeEvent<?, ?>> events = List.of(
                new CloudTaskCreatedEventImpl(assignedTask("task-1", "erin")),
                new CloudTaskCreatedEventImpl(assignedTask("task-2", "erin"))
            );
            messageHandler.accept(MessageBuilder.withPayload(events).build());

            Message<byte[]> received = receiveContaining(output, "assigned:erin", 3000);
            assertThat(received).isNotNull();
            assertThat(new String(received.getPayload(), StandardCharsets.UTF_8)).contains("assigned:erin");
            assertThat(receiveContaining(output, "processes:erin", 2000)).isNotNull();
        }
    }

    /** Skips messages that don't match. */
    private static Message<byte[]> receiveContaining(OutputDestination output, String needle, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        Message<byte[]> received;
        do {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return null;
            }
            received = output.receive(remaining, COUNT_DESTINATION);
        } while (received != null && !new String(received.getPayload(), StandardCharsets.UTF_8).contains(needle));
        return received;
    }

    private static TaskImpl assignedTask(String id, String assignee) {
        TaskImpl task = new TaskImpl();
        task.setId(id);
        task.setAssignee(assignee);
        return task;
    }
}
