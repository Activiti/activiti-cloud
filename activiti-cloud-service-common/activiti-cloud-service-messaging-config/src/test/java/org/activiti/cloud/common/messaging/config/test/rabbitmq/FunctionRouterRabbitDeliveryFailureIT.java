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
 * Real-broker regression test for the function router's handling of delivery failures
 * (executor queue overflow / shutdown) versus genuine connector execution failures.
 *
 * Unlike the other Function Router IT suites in this module (which import
 * {@code TestChannelBinderConfiguration} and never touch a real broker), this test runs against
 * a real RabbitMQ instance via {@link RabbitMQContainerApplicationInitializer}, so it exercises the
 * exact code path that produced the production incident: {@code CompletableFuture.supplyAsync()}
 * submitting to a saturated {@code FunctionRouterExecutorFactory} executor from a real AMQP listener
 * thread.
 *
 * Note on the queue-overflow scenario: {@code AmqpInboundChannelAdapter} wraps message handling in
 * its own {@code RetryTemplate}, independent of this module's {@code CompletableFutureRetry}. That
 * outer retry self-heals a transient queue-full condition regardless of whether the synchronous-throw
 * fix is applied, so it cannot be used to tell fixed and broken code apart. The executor-shutdown
 * scenario below is the one that matters: the failure never clears on its own, so the outer retry
 * eventually exhausts, and only then does the difference between "delivery failure filtered
 * internally" and "exception escapes to the global error channel" become observable.
 *
 * <p>Message-loss gap and its fix: for a delivery failure that never clears (executor permanently
 * shut down), the previous auto-ack behavior acknowledged the message as soon as the (fire-and-
 * forget) listener method returned, regardless of whether the work it kicked off actually
 * succeeded - {@code AmqpInboundChannelAdapter}'s own {@code RetryTemplate}/{@code RecoveryCallback}
 * ({@code ErrorMessageSendingRecoverer}) traps failures rather than rethrowing them once its own
 * retries exhaust, so the listener always returned normally and the container always acked,
 * confirmed via {@code rabbitmqctl list_queues} (queue depth dropped to 0, message never seen
 * again) regardless of whether this module's own synchronous-throw fix was applied. This is fixed
 * by switching the function-router's consumer to manual ack mode (see
 * {@code ActivitiCloudMessagingAutoConfiguration}'s listener container customizer) and having
 * {@code FunctionRouterConfiguration} explicitly ack on success/genuine-error and
 * nack-with-requeue=true on a genuine delivery failure, once it actually knows the outcome -
 * verified below by {@code should_absorbExecutorQueueOverflow_withoutErrorHandlerInvocation} (the
 * overflowing message is no longer dropped) and
 * {@code should_genuinelyRedeliverDeliveryFailure_soMessageSucceedsAfterExecutorRecovers} (a
 * permanently-failing delivery is genuinely redelivered by RabbitMQ and succeeds once the executor
 * recovers).
 *
 * <p>Deferring ack until this module's own async chain resolves means the underlying AMQP consumer
 * (default prefetch=1) won't fetch a connector's next message until its current one is acked -
 * {@code should_notBlockOtherConnectors_whileOneConnectorIsStillProcessing} confirms this only
 * throttles a single connector's own throughput and does not block unrelated connectors sharing
 * the function-router consumer group, since each connector binding gets its own listener
 * container/consumer. Separately, a delivery failure that never clears now produces a tight nack
 * -&gt; immediate broker redelivery -&gt; immediate re-rejection loop with no backoff (observed:
 * tens of thousands of attempts within seconds) for as long as the underlying condition persists -
 * a real operational consideration for a slow rolling upgrade, not addressed by this change.
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

    // The function-router registers a connector by matching the message's `connectorType` header
    // against the *destination* configured for its input binding - both must be the same literal
    // value, so the AMQP exchange/routing name doubles as the connector type identifier here.
    private static final String CONNECTOR_TYPE = "test-connector-queue";
    private static final String EXCHANGE = "test-connector-queue";
    private static final String THROW_MARKER = "THROW:";
    // FunctionRouterExecutorFactory keys its executors by the resolved function registration
    // name, i.e. the bean name plus Spring Cloud Function's registration suffix.
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
                } catch (InterruptedException e) {
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
        assertThat(TestConnectorState.processedPayloads).doesNotContain(payload);
    }

    @Test
    void should_absorbExecutorQueueOverflow_withoutErrorHandlerInvocation() throws InterruptedException {
        // Saturate the single-threaded, queue-size-1 executor backing this connector's
        // registration directly (rather than racing real AMQP delivery timing to fill it): one
        // task occupies the running slot, a second fills the queue. A message published while
        // both are occupied hits the executor's RejectedExecutionHandler synchronously once
        // dispatched by the real AMQP listener - the exact condition that produced
        // RejectedExecutionException in production during a rolling upgrade.
        //
        // AmqpInboundChannelAdapter wraps this dispatch in its own RetryTemplate: without this
        // module's fix, that outer retry engages (our code throws synchronously, exactly what
        // triggers it) and gives the condition several seconds across ~4 attempts to clear - long
        // enough for overflow-1 below to recover once we release the executor after 1s. With the
        // fix applied, our own handler never throws synchronously, so the outer retry never
        // engages at all; only this module's own much shorter internal retry
        // (CompletableFutureRetry, max-retries=1, 10ms) gets a chance, which exhausts almost
        // immediately - well before the 1s release - so overflow-1 is dropped instead (see the
        // class-level Javadoc). What IS consistent regardless of the fix is whether a delivery
        // failure is ever forwarded to the function router's error handler while retries are still
        // in play: it must not be, since that would incorrectly fail the waiting service task
        // instead of letting the retry (whichever layer's) play out.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);

        var runningTaskStarted = new CountDownLatch(1);
        var releaseRunningTask = new CountDownLatch(1);
        executor.submit(() -> {
            runningTaskStarted.countDown();
            try {
                releaseRunningTask.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
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

        // Give the real AMQP listener time to dispatch each overflow message to the saturated
        // executor and hit the RejectedExecutionHandler at least once.
        Thread.sleep(1000);

        // Free the executor so any overflow message still pending can be processed.
        releaseRunningTask.countDown();

        // overflow-1 (the message that actually hit the saturated queue) must also be genuinely
        // redelivered and eventually processed, not silently dropped - a delivery failure must
        // result in a real nack+requeue, not an optimistic ack of a message we haven't actually
        // finished handling yet.
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(TestConnectorState.processedPayloads).containsAll(overflowPayloads));

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    @Test
    void should_genuinelyRedeliverDeliveryFailure_soMessageSucceedsAfterExecutorRecovers() throws InterruptedException {
        // Reproduces the actual application-upgrade scenario end-to-end: the executor backing
        // this connector's registration becomes permanently unavailable (simulating the old
        // instance's executor shutting down) while a message is in flight, and later a *new*,
        // healthy executor becomes available for the same registration (simulating the new
        // instance/pod becoming ready). If the delivery failure was genuinely nacked and
        // requeued by RabbitMQ (rather than optimistically acked and dropped), the message
        // should be redelivered and processed successfully once the executor recovers.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);
        executor.shutdown();

        var payload = uniquePayload("recovers-after-shutdown");
        publish(payload);

        // Give every dispatch attempt (this module's own retry, and - in the broken state -
        // AmqpInboundChannelAdapter's outer retry) a chance to run against the permanently
        // shut-down executor and exhaust.
        Thread.sleep(12000);

        // Simulate recovery: destroy() clears FunctionRouterExecutorFactory's executor map, so
        // the next submission attempt for this registration creates a fresh, healthy executor -
        // standing in for a new/recovered instance taking over.
        functionRouterExecutorFactory.destroy();

        await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> assertThat(TestConnectorState.processedPayloads).contains(payload));

        assertThat(TestConnectorState.capturedErrors).isEmpty();
    }

    @Test
    void should_notLeakDeliveryFailureToGlobalErrorChannel_whenExecutorIsPermanentlyShutDown()
        throws InterruptedException {
        // Unlike a transiently full queue, a shut-down executor never recovers on its own - so
        // this reproduces the actual application-upgrade scenario: the old instance's executor is
        // shutting down while a message for that connector is still in flight. Every dispatch
        // attempt (including all of AmqpInboundChannelAdapter's own outer retries) fails the same
        // way, forever, since there is no second instance in this test to take over.
        //
        // This is the scenario that actually discriminates the fix: with the synchronous-throw
        // bug present, the ImmediateRequeueAmqpException escapes functionRouterMessageHandler()
        // uncaught, which - once the outer retry budget is exhausted - surfaces on Spring
        // Integration's global error channel as an unhandled exception. With the fix applied, it
        // is detected and filtered internally every time, so nothing ever reaches either the
        // function router's own error handler or the global error channel.
        var executor = functionRouterExecutorFactory.apply(REGISTRATION_NAME);
        executor.shutdown();

        var payload = uniquePayload("shutdown");
        publish(payload);

        // With manual ack, the message is now genuinely nacked and redelivered by RabbitMQ
        // throughout this window (rather than acked-and-dropped after one delivery), so it keeps
        // hitting the still-shut-down executor over and over. In the broken (synchronous-throw)
        // state, each of those redeliveries also exhausts AmqpInboundChannelAdapter's own outer
        // RetryTemplate (~4 in-process attempts with exponential backoff, ~7-8s total) before its
        // ErrorMessageSendingRecoverer traps the failure and publishes it to the global error
        // channel. Either way, one window past a full retry-exhaustion cycle is enough to observe
        // whether the failure ever leaks there.
        Thread.sleep(12000);

        assertThat(TestConnectorState.processedPayloads).doesNotContain(payload);
        assertThat(TestConnectorState.capturedErrors).isEmpty();
        assertThat(globalErrorChannelMessages)
            .as("no delivery failure should ever escape as an unhandled exception on the global error channel")
            .isEmpty();
    }

    @Test
    void should_notBlockOtherConnectors_whileOneConnectorIsStillProcessing() throws InterruptedException {
        // Manual ack defers acknowledgment until this module's own async chain resolves (see
        // acknowledge()/negativelyAcknowledgeAndRequeue() in FunctionRouterConfiguration), rather
        // than the previous fire-and-forget auto-ack that happened as soon as the listener
        // returned. RabbitMQ's default consumer prefetch is 1, so if the slow connector's message
        // being unacked blocked the shared function-router queue's consumer from fetching
        // anything else, an unrelated, fast connector's message would be stuck behind it too.
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
