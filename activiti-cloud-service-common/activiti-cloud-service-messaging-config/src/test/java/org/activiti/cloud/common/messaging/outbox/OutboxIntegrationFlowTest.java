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
package org.activiti.cloud.common.messaging.outbox;

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;
import static org.activiti.cloud.common.messaging.outbox.OutboxHeaders.OUTBOX_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringJUnitConfig(OutboxIntegrationFlowTest.TestConfig.class)
class OutboxIntegrationFlowTest {

    private static final String PARTITION_KEY = "partition-1";
    private static final Duration ACTIVE_PERIOD = Duration.ofMillis(50);
    private static final Duration IDLE_PERIOD = Duration.ofMillis(500);

    @Configuration
    @EnableIntegration
    static class TestConfig {}

    @Autowired
    private IntegrationFlowContext flowContext;

    @Autowired
    private ApplicationContext applicationContext;

    private QueueChannel queueChannel;
    private RecordingTransactionManager transactionManager;
    private StreamBridge streamBridge;
    private LockRegistry<? extends Lock> lockRegistry;
    private AtomicInteger errorAdviceInvocations;
    private IntegrationFlowRegistration registration;

    @BeforeEach
    void setUp() {
        queueChannel = new QueueChannel();

        OutboxProperties outboxProperties = new OutboxProperties();
        outboxProperties.setPollerActivePeriod(ACTIVE_PERIOD);
        outboxProperties.setPollerIdlePeriod(IDLE_PERIOD);

        transactionManager = new RecordingTransactionManager();
        streamBridge = mock(StreamBridge.class);

        // Real Spring ObjectProvider backed by a real bean factory
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("streamBridge", streamBridge);
        ObjectProvider<StreamBridge> streamBridgeProvider = beanFactory.getBeanProvider(StreamBridge.class);

        // Real in-memory LockRegistry from Spring Integration
        lockRegistry = new DefaultLockRegistry();

        errorAdviceInvocations = new AtomicInteger();
        Advice handlerErrorAdvice = (MethodInterceptor) invocation -> {
            errorAdviceInvocations.incrementAndGet();
            return invocation.proceed();
        };

        final var factory = new OutboxIntegrationFlowFactory(
            outboxProperties,
            transactionManager,
            streamBridgeProvider,
            lockRegistry,
            handlerErrorAdvice
        );

        final var outboxIntegrationFlow = factory.apply(PARTITION_KEY, queueChannel);

        registration = flowContext
            .registration(outboxIntegrationFlow)
            .id(PARTITION_KEY + ".flow")
            .autoStartup(false)
            .register();
    }

    @AfterEach
    void tearDown() {
        if (registration != null) {
            flowContext.remove(registration.getId());
        }
    }

    @Test
    @DisplayName(
        "Should configure bridge endpoint with partition ID, outbox role, daemon scheduler, and autoStartup=false"
    )
    void shouldConfigureBridgeEndpointProperties() {
        String expectedEndpointId = PARTITION_KEY + OUTBOX_ROLE;

        assertThat(applicationContext.containsBean(expectedEndpointId))
            .as("Bridge endpoint bean should be registered with partitionKey + OUTBOX_ROLE")
            .isTrue();

        PollingConsumer endpoint = applicationContext.getBean(expectedEndpointId, PollingConsumer.class);

        assertThat(endpoint.getComponentName()).isEqualTo(expectedEndpointId);
        assertThat(endpoint.getRole()).isEqualTo(OUTBOX_ROLE);
        assertThat(endpoint.isAutoStartup()).isFalse();
        assertThat(endpoint.isRunning()).isFalse();
        assertThat(endpoint.getMaxMessagesPerPoll()).isEqualTo(1);

        DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
        TaskScheduler scheduler = (TaskScheduler) endpointAccessor.getPropertyValue("taskScheduler");
        assertThat(scheduler).isInstanceOf(ConcurrentTaskScheduler.class);
    }

    @Test
    @DisplayName("Should poll message, commit transaction, and send to StreamBridge using OUTBOX_DESTINATION header")
    void shouldPollAndSendMessageToStreamBridgeWhenStarted() {
        registration.start();

        String destination = "orders-out-0";
        Message<String> testMessage = MessageBuilder.withPayload("outbox-payload")
            .setHeader(OUTBOX_DESTINATION, destination)
            .build();

        queueChannel.send(testMessage);

        await()
            .atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat(queueChannel.getQueueSize()).isZero();
                assertThat(errorAdviceInvocations).hasValue(1);
                assertThat(transactionManager.committedCount()).isGreaterThanOrEqualTo(1);
                assertThat(transactionManager.rolledBackCount()).isZero();

                ArgumentCaptor<Message<?>> argumentCaptor = ArgumentCaptor.captor();
                verify(streamBridge).send(eq(destination), argumentCaptor.capture());

                assertThat(argumentCaptor.getValue().getPayload()).isEqualTo("outbox-payload");
                assertThat(argumentCaptor.getValue().getHeaders().get(OUTBOX_DESTINATION)).isEqualTo(destination);
            });
    }

    @Test
    @DisplayName(
        "Should rollback transaction and route DestinationResolutionException to errorChannel when OUTBOX_DESTINATION is missing"
    )
    void shouldRouteErrorToErrorChannelWhenDestinationHeaderIsMissing() {
        registration.start();

        Message<?> invalidMessage = MessageBuilder.withPayload("missing-destination-payload").build();

        queueChannel.send(invalidMessage);

        await()
            .atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                verify(streamBridge, never()).send(anyString(), any(Message.class));
                assertThat(transactionManager.rolledBackCount()).isGreaterThanOrEqualTo(1);
            });
    }

    /**
     * Real AbstractPlatformTransactionManager implementation that tracks transaction lifecycle counts.
     */
    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private final AtomicInteger begun = new AtomicInteger();
        private final AtomicInteger committed = new AtomicInteger();
        private final AtomicInteger rolledBack = new AtomicInteger();

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
            begun.incrementAndGet();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
            committed.incrementAndGet();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
            rolledBack.incrementAndGet();
        }

        int begunCount() {
            return begun.get();
        }

        int committedCount() {
            return committed.get();
        }

        int rolledBackCount() {
            return rolledBack.get();
        }
    }
}
