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
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.app.AssignedTaskCounter;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * End-to-end across the module boundary this refactor introduced: a real committed event, flowing
 * through the real {@code engineEvents} -&gt; query-consumer -&gt; {@code queryEvents} path (unchanged),
 * captured by the new pushed-counts module's own {@code queryEvents} subscription, resolved against
 * a directly-registered {@link SubscriberRegistry} entry, counted by a fixed-result
 * {@link AssignedTaskCounter} test double, and delivered straight into the shared
 * {@code Flux<CountChangedMessage>} - no broker relay involved.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "activiti.cloud.query.pushed-counts.enabled=true",
        "activiti.features.query.pushed-counts.enabled=true",
        "activiti.cloud.query.pushed-counts.flush-interval=PT0.1S",
        "activiti.cloud.query.pushed-counts.flush-max-window=PT0.2S",
    }
)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import({ TestChannelBinderConfiguration.class, PushedCountsEndToEndIT.TestCounterConfig.class })
class PushedCountsEndToEndIT {

    private static final String ALICE = "pushed-counts-e2e-alice";
    private static final String HENRY = "pushed-counts-e2e-henry";

    @Autowired
    private MyProducer producer;

    @Autowired
    private SubscriberRegistry subscriberRegistry;

    @Autowired
    private Flux<CountChangedMessage> pushedCountsFlux;

    @MockitoBean
    private BuildProperties buildProperties;

    private final CopyOnWriteArrayList<CountChangedMessage> received = new CopyOnWriteArrayList<>();
    private Disposable subscription;

    @BeforeEach
    void subscribe() {
        subscription = pushedCountsFlux.subscribe(received::add);
    }

    @AfterEach
    void tearDown() {
        subscription.dispose();
        subscriberRegistry.unregister(ALICE, "test-session");
        subscriberRegistry.unregister(HENRY, "test-session");
    }

    @Test
    void committedTaskEvents_flowThroughQueryEvents_toACountChangedMessage_forAWatchingUser() {
        subscriberRegistry.register(ALICE, Set.of(), "test-session", Instant.now());

        TaskImpl task = new TaskImpl();
        task.setId("pushed-counts-e2e-task");
        task.setAssignee(ALICE);

        producer.send(
            new CloudTaskCreatedEventImpl(task),
            new CloudTaskAssignedEventImpl("pushed-counts-e2e-evt", System.currentTimeMillis(), task)
        );

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                assertThat(received).anyMatch(
                    message -> message.scopeKey().equals("assigned:" + ALICE) && message.count() == 5
                )
            );
    }

    @Test
    void committedProcessStartedEvent_flowsThroughQueryEvents_toACountChangedMessage_forAWatchingInitiator() {
        subscriberRegistry.register(HENRY, Set.of(), "test-session", Instant.now());

        CloudProcessInstanceImpl process = new CloudProcessInstanceImpl();
        process.setId("pushed-counts-e2e-process");
        process.setInitiator(HENRY);

        // ProcessStartedEventHandler requires the row to already exist.
        producer.send(new CloudProcessCreatedEventImpl(process), new CloudProcessStartedEventImpl(process));

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                assertThat(received).anyMatch(message -> message.scopeKey().equals("processes:" + HENRY))
            );
    }

    @TestConfiguration
    static class TestCounterConfig {

        @Bean
        AssignedTaskCounter testAssignedCounter(TaskRepository taskRepository) {
            return new AssignedTaskCounter(taskRepository) {
                @Override
                public Map<String, Long> compute(Set<String> affectedUserIds) {
                    return Map.of(ALICE, 5L);
                }
            };
        }
    }

    /**
     * A separate context (own properties): {@code buffer-hard-cap=1}, so the second of two tasks
     * captured in the same batch is dropped rather than counted.
     */
    @Nested
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "activiti.cloud.query.pushed-counts.enabled=true",
            "activiti.features.query.pushed-counts.enabled=true",
            "activiti.cloud.query.pushed-counts.flush-interval=PT0.1S",
            "activiti.cloud.query.pushed-counts.flush-max-window=PT0.2S",
            "activiti.cloud.query.pushed-counts.buffer-hard-cap=1",
        }
    )
    @TestPropertySource("classpath:application-test.properties")
    @ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
    @Import({ TestChannelBinderConfiguration.class, PushedCountsEndToEndIT.TestCounterConfig.class })
    class HardCapScenario {

        private static final String FRANK = "pushed-counts-e2e-frank";
        private static final String GRACE = "pushed-counts-e2e-grace";

        @Autowired
        private MyProducer hardCapProducer;

        @Autowired
        private SubscriberRegistry hardCapSubscriberRegistry;

        @Autowired
        private Flux<CountChangedMessage> hardCapPushedCountsFlux;

        private final CopyOnWriteArrayList<CountChangedMessage> hardCapReceived = new CopyOnWriteArrayList<>();
        private Disposable hardCapSubscription;

        @BeforeEach
        void subscribe() {
            hardCapSubscription = hardCapPushedCountsFlux.subscribe(hardCapReceived::add);
        }

        @AfterEach
        void tearDown() {
            hardCapSubscription.dispose();
            hardCapSubscriberRegistry.unregister(FRANK, "test-session");
            hardCapSubscriberRegistry.unregister(GRACE, "test-session");
        }

        @Test
        void touchingMoreTasksThanTheHardCap_dropsTheOverflow_soOnlyTheFirstIsCounted() {
            hardCapSubscriberRegistry.register(FRANK, Set.of(), "test-session", Instant.now());
            hardCapSubscriberRegistry.register(GRACE, Set.of(), "test-session", Instant.now());

            TaskImpl frankTask = new TaskImpl();
            frankTask.setId("pushed-counts-e2e-hardcap-task-1");
            frankTask.setAssignee(FRANK);
            TaskImpl graceTask = new TaskImpl();
            graceTask.setId("pushed-counts-e2e-hardcap-task-2");
            graceTask.setAssignee(GRACE);

            hardCapProducer.send(new CloudTaskCreatedEventImpl(frankTask), new CloudTaskCreatedEventImpl(graceTask));

            await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                    assertThat(hardCapReceived).anyMatch(message -> message.scopeKey().equals("assigned:" + FRANK))
                );
            // Give a delayed Grace message - from a scheduler flush racing the capture of this
            // same batch - a real chance to surface before asserting she was never counted.
            await()
                .during(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(2))
                .until(() ->
                    hardCapReceived.stream().noneMatch(message -> message.scopeKey().equals("assigned:" + GRACE))
                );
        }
    }

    /**
     * A long {@code flush-max-window} (10s) with a tiny {@code flush-max-size} (2), so a flush
     * arriving within a few seconds can only be explained by the size trigger, not the window.
     */
    @Nested
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "activiti.cloud.query.pushed-counts.enabled=true",
            "activiti.features.query.pushed-counts.enabled=true",
            "activiti.cloud.query.pushed-counts.flush-interval=PT0.1S",
            "activiti.cloud.query.pushed-counts.flush-max-window=PT10S",
            "activiti.cloud.query.pushed-counts.flush-max-size=2",
            "activiti.cloud.query.pushed-counts.buffer-hard-cap=100",
        }
    )
    @TestPropertySource("classpath:application-test.properties")
    @ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
    @Import({ TestChannelBinderConfiguration.class, PushedCountsEndToEndIT.TestCounterConfig.class })
    class SoftCapScenario {

        private static final String ERIN = "pushed-counts-e2e-erin";

        @Autowired
        private MyProducer softCapProducer;

        @Autowired
        private SubscriberRegistry softCapSubscriberRegistry;

        @Autowired
        private Flux<CountChangedMessage> softCapPushedCountsFlux;

        private final CopyOnWriteArrayList<CountChangedMessage> softCapReceived = new CopyOnWriteArrayList<>();
        private Disposable softCapSubscription;

        @BeforeEach
        void subscribe() {
            softCapSubscription = softCapPushedCountsFlux.subscribe(softCapReceived::add);
        }

        @AfterEach
        void tearDown() {
            softCapSubscription.dispose();
            softCapSubscriberRegistry.unregister(ERIN, "test-session");
        }

        @Test
        void touchingAsManyTasksAsTheSoftCap_flushesEarly_withoutWaitingOutTheMuchLongerWindow() {
            softCapSubscriberRegistry.register(ERIN, Set.of(), "test-session", Instant.now());

            TaskImpl task1 = new TaskImpl();
            task1.setId("pushed-counts-e2e-softcap-task-1");
            task1.setAssignee(ERIN);
            TaskImpl task2 = new TaskImpl();
            task2.setId("pushed-counts-e2e-softcap-task-2");
            task2.setAssignee(ERIN);

            softCapProducer.send(new CloudTaskCreatedEventImpl(task1), new CloudTaskCreatedEventImpl(task2));

            await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() ->
                    assertThat(softCapReceived).anyMatch(message -> message.scopeKey().equals("assigned:" + ERIN))
                );
        }
    }
}
