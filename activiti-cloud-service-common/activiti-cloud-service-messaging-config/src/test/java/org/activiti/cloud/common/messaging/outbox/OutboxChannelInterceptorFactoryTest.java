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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.function.BiFunction;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class OutboxChannelInterceptorFactoryTest {

    @Spy
    private OutboxProperties outboxProperties = new OutboxProperties();

    @Mock
    private ChannelMessageStore channelMessageStore;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ObjectProvider<StreamBridge> streamBridgeProvider;

    @Mock
    private LockRegistry lockRegistry;

    @Mock
    private Advice outboxMessageHandlerErrorAdvice;

    private OutboxIntegrationFlowFactory outboxIntegrationFlowFactory;

    @Mock
    private BiFunction<Message<?>, MessageChannel, String> destinationProvider;

    private AnnotationConfigApplicationContext applicationContext;
    private IntegrationFlowContext integrationFlowContext;
    private OutboxChannelInterceptorFactory factory;

    @BeforeEach
    void setUp() {
        outboxProperties.setPollerActivePeriod(Duration.ofMillis(100));
        outboxProperties.setPollerIdlePeriod(Duration.ofSeconds(5));

        applicationContext = new AnnotationConfigApplicationContext(IntegrationTestConfig.class);
        integrationFlowContext = applicationContext.getBean(IntegrationFlowContext.class);

        outboxIntegrationFlowFactory = new OutboxIntegrationFlowFactory(
            outboxProperties,
            transactionManager,
            streamBridgeProvider,
            lockRegistry,
            outboxMessageHandlerErrorAdvice
        );

        factory = new OutboxChannelInterceptorFactory(
            outboxProperties,
            channelMessageStore,
            outboxIntegrationFlowFactory,
            integrationFlowContext
        );
    }

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void shouldCreateAndRegisterOutboxChannelInterceptorForPartition() {
        // Given
        String partitionKey = "orders";

        // When
        ChannelInterceptor interceptor = factory.apply(partitionKey, destinationProvider);

        // Then
        assertThat(interceptor).isNotNull().isInstanceOf(OutboxChannelInterceptor.class);

        IntegrationFlowRegistration registration = integrationFlowContext.getRegistrationById("ordersOutboxFlow");
        assertThat(registration).isNotNull();
        assertThat(applicationContext.containsBean("ordersQueue")).isTrue();

        Lifecycle endpoint = applicationContext.getBean("orders" + OUTBOX_ROLE, Lifecycle.class);
        assertThat(endpoint.isRunning()).isFalse();
    }

    @Test
    void shouldCacheAndReuseInterceptorForSamePartitionKey() {
        // Given
        String partitionKey = "orders";

        // When
        ChannelInterceptor first = factory.apply(partitionKey, destinationProvider);
        ChannelInterceptor second = factory.apply(partitionKey, destinationProvider);

        // Then
        assertThat(second).isSameAs(first);
        assertThat(integrationFlowContext.getRegistry()).hasSize(1);
    }

    @Test
    void shouldManageSmartLifecycleAndStopAllRegisteredFlowsOnStop() {
        // Given
        factory.apply("partition1", destinationProvider);
        factory.apply("partition2", destinationProvider);

        IntegrationFlowRegistration registration1 = integrationFlowContext.getRegistrationById("partition1OutboxFlow");
        IntegrationFlowRegistration registration2 = integrationFlowContext.getRegistrationById("partition2OutboxFlow");

        Lifecycle endpoint1 = applicationContext.getBean("partition1" + OUTBOX_ROLE, Lifecycle.class);
        Lifecycle endpoint2 = applicationContext.getBean("partition2" + OUTBOX_ROLE, Lifecycle.class);

        assertThat(factory.isRunning()).isFalse();

        // When: start() is called
        factory.start();
        factory.start();

        // Then
        assertThat(factory.isRunning()).isTrue();

        // Start registrations explicitly to verify factory.stop() stops them
        registration1.start();
        registration2.start();
        assertThat(endpoint1.isRunning()).isTrue();
        assertThat(endpoint2.isRunning()).isTrue();

        // When: stop() is called
        factory.stop();
        factory.stop();

        // Then
        assertThat(factory.isRunning()).isFalse();
        assertThat(endpoint1.isRunning()).isFalse();
        assertThat(endpoint2.isRunning()).isFalse();
    }

    @Configuration
    @EnableIntegration
    static class IntegrationTestConfig {}
}
