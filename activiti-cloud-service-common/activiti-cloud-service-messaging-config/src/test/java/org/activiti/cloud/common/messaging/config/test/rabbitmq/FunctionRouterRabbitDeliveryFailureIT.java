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
package org.activiti.cloud.common.messaging.config.test.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.activiti.cloud.common.messaging.config.FunctionRouterExecutorFactory;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQQueuesCleanupTestExecutionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

/**
 * Real-broker (RabbitMQ Testcontainer) regression test for the function router's handling of
 * delivery failures (executor queue overflow / shutdown) versus genuine connector execution
 * failures - the production code path that {@code TestChannelBinderConfiguration}-based suites
 * cannot reproduce.
 *
 * <p>With manual ack, a delivery failure is nacked-with-requeue rather than acked-and-dropped, so
 * the message is genuinely redelivered and eventually processed once the executor recovers, and it
 * never leaks to the function router's error handler or the global error channel. Since ack is
 * deferred until the async chain resolves (consumer prefetch=1), a permanently failing delivery
 * loops nack -&gt; redelivery -&gt; re-rejection with no backoff - a known operational trade-off,
 * not addressed here.
 */
@SpringBootTest(
    classes = { FunctionRouterRabbitTestApplication.class },
    properties = {
        "activiti.cloud.application.name=functest",
        "spring.application.name=functest",
        "activiti.cloud.messaging.destination-transformers-enabled=false",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.error-handler-definition=testErrorHandler",
        "activiti.cloud.messaging.function-router.request-timeout=300ms",
        "activiti.cloud.messaging.function-router.max-retries=1",
        "activiti.cloud.messaging.function-router.retry-interval=10ms",
        "spring.cloud.stream.bindings.testConnectorInput.destination=test-connector-queue",
        "spring.cloud.stream.bindings.testConnectorInput.group=test-connector-group",
        "spring.cloud.stream.bindings.testConnectorInput.content-type=text/plain",
        "activiti.cloud.messaging.function-router.routes.testConnectorInput.enabled=true",
        "spring.cloud.stream.bindings.slowConnectorInput.destination=slow-connector-queue",
        "spring.cloud.stream.bindings.slowConnectorInput.group=slow-connector-group",
        "spring.cloud.stream.bindings.slowConnectorInput.content-type=text/plain",
        "activiti.cloud.messaging.function-router.routes.slowConnectorInput.enabled=true",
    }
)
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class })
@TestExecutionListeners(
    value = RabbitMQQueuesCleanupTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
@Import(FunctionRouterRabbitDeliveryFailureIT.TestConnectorConfig.class)
class FunctionRouterRabbitDeliveryFailureIT {

    // connectorType must equal the input binding's destination for the router to match it
    private static final String CONNECTOR_TYPE = "test-connector-queue";
    private static final String EXCHANGE = "test-connector-queue";
    private static final String THROW_MARKER = "THROW:";
    // executors are keyed by registration name: bean name + Spring Cloud Function suffix
    private static final String REGISTRATION_NAME = "testConnector_registration";

    private static final String SLOW_CONNECTOR_TYPE = "slow-connector-queue";
    private static final String SLOW_EXCHANGE = "slow-connector-queue";

    @TestConfiguration
    static class TestConnectorConfig {

        @InputBinding("testConnectorInput")
        SubscribableChannel testConnectorInput() {
            return MessageChannels.publishSubscribe("testConnectorInput").getObject();
        }

        @Bean("testConnector")
        @ConnectorBinding(input = "testConnectorInput", connectorType = CONNECTOR_TYPE, condition = "")
        Consumer<org.springframework.messaging.Message<String>> testConnector() {
            return TestConnectorState::handle;
        }

        @InputBinding("slowConnectorInput")
        SubscribableChannel slowConnectorInput() {
            return MessageChannels.publishSubscribe("slowConnectorInput").getObject();
        }

        @Bean("slowConnector")
        @ConnectorBinding(input = "slowConnectorInput", connectorType = SLOW_CONNECTOR_TYPE, condition = "")
        Consumer<org.springframework.messaging.Message<String>> slowConnector() {
            return TestConnectorState::handleSlow;
        }

        @Bean("testErrorHandler")
        Consumer<ErrorMessage> testErrorHandler() {
            return TestConnectorState::recordError;
        }
    }

    /**
     * Static because the connector bean and this test class are instantiated independently by
     * Spring; both need to observe the same invocation/error state.
     */
    static final class TestConnectorState {

        static final CopyOnWriteArrayList<String> processedPayloads = new CopyOnWriteArrayList<>();
        static final CopyOnWriteArrayList<ErrorMessage> capturedErrors = new CopyOnWriteArrayList<>();
        static final AtomicReference<CountDownLatch> slowConnectorGate = new AtomicReference<>();
        static final AtomicReference<CountDownLatch> slowConnectorStarted = new AtomicReference<>(
            new CountDownLatch(1)
        );

        static void handle(org.springframework.messaging.Message<String> message) {
            var payload = message.getPayload();

            if (payload.startsWith(THROW_MARKER)) {
                throw new RuntimeException("boom: " + payload);
            }

            processedPayloads.add(payload);
        }

        static void handleSlow(org.springframework.messaging.Message<String> message) {
            slowConnectorStarted.get().countDown();
            var gate = slowConnectorGate.get();
            if (gate != null) {
                try {
                    gate.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
            processedPayloads.add(message.getPayload());
        }

        static void recordError(ErrorMessage errorMessage) {
            capturedErrors.add(errorMessage);
        }

        static void reset() {
            processedPayloads.clear();
            capturedErrors.clear();
            slowConnectorGate.set(null);
            slowConnectorStarted.set(new CountDownLatch(1));
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private FunctionRouterExecutorFactory functionRouterExecutorFactory;

    @Autowired
    @Qualifier("errorChannel")
    private SubscribableChannel globalErrorChannel;

    private final CopyOnWriteArrayList<org.springframework.messaging.Message<?>> globalErrorChannelMessages =
        new CopyOnWriteArrayList<>();
    private final MessageHandler globalErrorChannelSpy = globalErrorChannelMessages::add;

    @BeforeEach
    void setUp() {
        TestConnectorState.reset();
        globalErrorChannelMessages.clear();
        globalErrorChannel.subscribe(globalErrorChannelSpy);
    }

    @AfterEach
    void tearDown() {
        globalErrorChannel.unsubscribe(globalErrorChannelSpy);
    }

    @Test
    void should_processMessage_when_executorHasCapacity() {
        var payload = uniquePayload("happy-path");

        publish(payload);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(TestConnectorState.processedPayloads).contains(payload));

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    @Test
    void should_invokeErrorHandler_when_connectorThrowsGenuineException() {
        var payload = THROW_MARKER + uniquePayload("execution-failure");

        publish(payload);

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(TestConnectorState.capturedErrors).hasSize(1));

        assertThat(TestConnectorState.capturedErrors.get(0).getPayload()).hasRootCauseMessage("boom: " + payload);

        // A genuine execution failure must not be silently swallowed as if it were a delivery failure.
        assertThat(payload).isNotIn(TestConnectorState.processedPayloads);
    }

    @Test
    void should_absorbExecutorQueueOverflow_withoutErrorHandlerInvocation() throws InterruptedException {
        // Saturate the single-threaded, queue-size-1 executor directly (1 task running, 1 queued);
        // a message published now hits the RejectedExecutionHandler synchronously on the AMQP
        // listener thread - the exact rolling-upgrade condition. The delivery failure must be
        // requeued and eventually processed, and never forwarded to the error handler.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);

        var runningTaskStarted = new CountDownLatch(1);
        var releaseRunningTask = new CountDownLatch(1);
        executor.submit(() -> {
            runningTaskStarted.countDown();
            try {
                releaseRunningTask.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(runningTaskStarted.await(5, TimeUnit.SECONDS))
            .as("directly-submitted task should have started running")
            .isTrue();
        executor.submit(() -> {}); // fills the single queue slot

        // Sanity check: confirm the executor is genuinely saturated before relying on real AMQP
        // delivery timing to reproduce the rejection.
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .as("executor should be saturated (1 running + 1 queued) at this point")
            .isInstanceOf(RejectedExecutionException.class);

        var overflowPayloads = List.of(
            uniquePayload("overflow-1"),
            uniquePayload("overflow-2"),
            uniquePayload("overflow-3")
        );
        overflowPayloads.forEach(this::publish);

        // nothing should be processed yet: the running task still holds the executor
        await()
            .during(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(2))
            .until(() -> overflowPayloads.stream().noneMatch(TestConnectorState.processedPayloads::contains));

        // free the executor so requeued messages can be processed
        releaseRunningTask.countDown();

        // every overflow message must be genuinely redelivered and processed, not dropped
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(TestConnectorState.processedPayloads).containsAll(overflowPayloads));

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    @Test
    void should_genuinelyRedeliverDeliveryFailure_soMessageSucceedsAfterExecutorRecovers() {
        // Rolling-upgrade scenario: the executor is permanently shut down while a message is in
        // flight, then a fresh executor becomes available (new pod). The requeued message should be
        // redelivered and succeed once the executor recovers.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);
        executor.shutdown();

        var payload = uniquePayload("recovers-after-shutdown");
        publish(payload);

        // while the executor is down the message must keep being requeued, never processed
        await()
            .during(Duration.ofSeconds(12))
            .atMost(Duration.ofSeconds(13))
            .until(() -> !TestConnectorState.processedPayloads.contains(payload));

        // recovery: destroy() clears the executor map so the next submission creates a fresh one
        functionRouterExecutorFactory.destroy();

        await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> assertThat(TestConnectorState.processedPayloads).contains(payload));

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    @Test
    void should_notLeakDeliveryFailureToGlobalErrorChannel_whenExecutorIsPermanentlyShutDown() {
        // A shut-down executor never recovers, so the delivery keeps failing. The failure must be
        // filtered internally on every attempt and never leak to the function router's error
        // handler or Spring Integration's global error channel.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);
        executor.shutdown();

        var payload = uniquePayload("shutdown");
        publish(payload);

        // poll throughout a full retry-exhaustion window so any leak fails fast
        await()
            .during(Duration.ofSeconds(12))
            .atMost(Duration.ofSeconds(13))
            .until(
                () ->
                    TestConnectorState.capturedErrors.isEmpty() &&
                    globalErrorChannelMessages.isEmpty() &&
                    !TestConnectorState.processedPayloads.contains(payload)
            );

        assertThat(payload).isNotIn(TestConnectorState.processedPayloads);
        assertThat(TestConnectorState.capturedErrors).isEmpty();
        assertThat(globalErrorChannelMessages)
            .as("no delivery failure should ever escape as an unhandled exception on the global error channel")
            .isEmpty();
    }

    @Test
    void should_notBlockOtherConnectors_whileOneConnectorIsStillProcessing() throws InterruptedException {
        // Manual ack defers acknowledgment until processing completes (prefetch=1). Verify a slow
        // connector holding its message unacked does not block an unrelated fast connector, since
        // each connector binding gets its own listener container/consumer.
        TestConnectorState.slowConnectorGate.set(new CountDownLatch(1));

        publishSlow(uniquePayload("slow"));
        assertThat(TestConnectorState.slowConnectorStarted.get().await(5, TimeUnit.SECONDS))
            .as("slow connector should have started processing and be holding its message unacked")
            .isTrue();

        var fastPayload = uniquePayload("fast-while-slow-in-flight");
        publish(fastPayload);

        try {
            await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                    assertThat(TestConnectorState.processedPayloads)
                        .as("a message for a different, fast connector must not be blocked behind a slow one")
                        .contains(fastPayload)
                );
        } finally {
            TestConnectorState.slowConnectorGate.get().countDown();
        }

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    private String uniquePayload(String label) {
        return label + "-" + UUID.randomUUID();
    }

    private void publish(String payload) {
        publish(EXCHANGE, CONNECTOR_TYPE, payload);
    }

    private void publishSlow(String payload) {
        publish(SLOW_EXCHANGE, SLOW_CONNECTOR_TYPE, payload);
    }

    private void publish(String exchange, String connectorType, String payload) {
        var messageProperties = new MessageProperties();
        messageProperties.setContentType("text/plain");
        messageProperties.setHeader("connectorType", connectorType);

        var amqpMessage = new Message(payload.getBytes(StandardCharsets.UTF_8), messageProperties);
        rabbitTemplate.send(exchange, "", amqpMessage);
    }
}
