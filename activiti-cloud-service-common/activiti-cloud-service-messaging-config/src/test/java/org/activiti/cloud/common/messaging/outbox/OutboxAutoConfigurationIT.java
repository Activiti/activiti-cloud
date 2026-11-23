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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.aop.SimpleActiveIdleReceiveMessageAdvice;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
    },
    classes = { OutboxAutoConfigurationIT.ApplicationConfig.class }
)
@TestPropertySource(properties = { "activiti.cloud.messaging.outbox.enabled=true", "spring.sql.init.platform=h2" })
@EnableTestBinder
@SpringBootApplication
class OutboxAutoConfigurationIT {

    private static final String AUDIT_PRODUCER = "auditProducer";

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private MessageChannel auditProducer;

    @Autowired
    private QueueChannel outboxDeadLetterQueue;

    @Autowired
    private QueueChannel auditProducerQueue;

    @Autowired
    private ChannelMessageStore outboxChannelMessageStore;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @MockitoSpyBean
    private StreamBridge streamBridge;

    @BeforeEach
    void setUp() {
        outboxChannelMessageStore.removeMessageGroup("outboxErrorMessages");
    }

    @TestConfiguration
    static class ApplicationConfig {

        @OutputBinding(AUDIT_PRODUCER)
        MessageChannel auditProducer() {
            return new DirectChannel();
        }
    }

    @Test
    void contextLoads() {}

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings()).isNotEmpty();
    }

    @Test
    void outboxProperties() {
        assertThat(outboxProperties.isEnabled()).isTrue();
        assertThat(outboxProperties.getRetryableExceptions())
            .containsEntry(MessageDispatchingException.class, true)
            .containsEntry(DestinationResolutionException.class, false);
        assertThat(outboxProperties.getCircuitBreakerOpenPeriod()).isEqualTo(Duration.ofSeconds(1));
        assertThat(outboxProperties.getCircuitBreakerThreshold()).isEqualTo(3);
    }

    @Test
    @DisplayName(
        "Should configure bridge endpoint with partition ID, outbox role, daemon scheduler, and autoStartup=false"
    )
    void shouldConfigureBridgeEndpointProperties() {
        String expectedEndpointId = AUDIT_PRODUCER.concat(OUTBOX_ROLE);

        assertThat(applicationContext.containsBean(expectedEndpointId))
            .as("Bridge endpoint bean should be registered with partitionKey + OUTBOX_ROLE")
            .isTrue();

        PollingConsumer endpoint = applicationContext.getBean(expectedEndpointId, PollingConsumer.class);

        assertThat(endpoint.getComponentName()).isEqualTo(expectedEndpointId);
        assertThat(endpoint.getRole()).isEqualTo(OUTBOX_ROLE);
        assertThat(endpoint.isAutoStartup()).isFalse();
        assertThat(endpoint.isRunning()).isTrue();
        assertThat(endpoint.getMaxMessagesPerPoll()).isEqualTo(1);

        DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
        TaskScheduler scheduler = (TaskScheduler) endpointAccessor.getPropertyValue("taskScheduler");
        assertThat(scheduler).isInstanceOf(ConcurrentTaskScheduler.class);
    }

    @Test
    @DisplayName("Should configure poller with DynamicPeriodicTrigger, error channel, transaction, and advice chain")
    @SuppressWarnings("unchecked")
    void shouldConfigurePollerMetadataAndAdviceChain() {
        PollingConsumer endpoint = applicationContext.getBean(
            AUDIT_PRODUCER.concat(OUTBOX_ROLE),
            PollingConsumer.class
        );
        DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);

        assertThat(endpoint.getMaxMessagesPerPoll()).isEqualTo(1L);
        assertThat(accessor.getPropertyValue("receiveTimeout")).isEqualTo(0L);

        assertThat(accessor.getPropertyValue("trigger")).isInstanceOfSatisfying(DynamicPeriodicTrigger.class, trigger ->
            assertThat(trigger.getDuration()).isEqualTo(outboxProperties.getPollerIdlePeriod())
        );

        List<Advice> adviceChain = (List<Advice>) accessor.getPropertyValue("adviceChain");
        assertThat(adviceChain).isNotNull().hasSize(4);

        assertThat(adviceChain.get(0)).isInstanceOf(CircuitBreakerPollerAdvice.class);
        assertThat(adviceChain.get(1)).isInstanceOf(PollLockAdvice.class);
        assertThat(adviceChain.get(2)).isInstanceOfSatisfying(
            SimpleActiveIdleReceiveMessageAdvice.class,
            activeIdleAdvice -> {
                DirectFieldAccessor adviceAccessor = new DirectFieldAccessor(activeIdleAdvice);
                assertThat(adviceAccessor.getPropertyValue("activePollPeriod")).isEqualTo(
                    outboxProperties.getPollerActivePeriod()
                );
                assertThat(adviceAccessor.getPropertyValue("idlePollPeriod")).isEqualTo(
                    outboxProperties.getPollerIdlePeriod()
                );
            }
        );
        assertThat(adviceChain.get(3)).isInstanceOf(TransactionInterceptor.class);
    }

    @Test
    @DisplayName("Should poll message, commit transaction, and send to StreamBridge using OUTBOX_DESTINATION header")
    void shouldPollAndSendMessageToOutputBindingDestination() {
        String destination = AUDIT_PRODUCER;
        Message<?> testMessage = MessageBuilder.withPayload("outbox-payload".getBytes()).build();

        auditProducer.send(testMessage);

        await().untilAsserted(() -> {
            ArgumentCaptor<Message<?>> argumentCaptor = ArgumentCaptor.captor();
            verify(streamBridge).send(eq(destination), argumentCaptor.capture());

            assertThat(argumentCaptor.getValue().getPayload()).isEqualTo("outbox-payload".getBytes());
            assertThat(argumentCaptor.getValue().getHeaders()).containsEntry(OUTBOX_DESTINATION, destination);
        });

        final var result = assertThat(outputDestination.receive(Duration.ofSeconds(1).toMillis(), "engineEvents"))
            .isNotNull()
            .actual();

        assertThat(result.getPayload()).isEqualTo("outbox-payload".getBytes());
        assertThat(result.getHeaders()).doesNotContainKey(OUTBOX_DESTINATION);
        assertThat(auditProducerQueue.getQueueSize()).isZero();
    }

    @Test
    @DisplayName(
        "Should rollback transaction, open circuit breaker and recover when stream bridge dispatch fails too many times in a row"
    )
    void shouldPollAndRetryAndTriggerCircuitBreakerAndRecoverAndSendMessageToOutputBindingDestination() {
        final var destination = AUDIT_PRODUCER;
        Message<?> testMessage = MessageBuilder.withPayload("outbox-payload".getBytes()).build();

        doReturn(false, false, false, false, false)
            .doCallRealMethod()
            .when(streamBridge)
            .send(eq(destination), any(Message.class));

        auditProducer.send(testMessage);

        ArgumentCaptor<Message<?>> argumentCaptor = ArgumentCaptor.captor();

        await()
            .forever()
            .untilAsserted(() -> {
                verify(streamBridge, times(6)).send(eq(destination), argumentCaptor.capture());
                assertThat(auditProducerQueue.getQueueSize()).isZero();
                assertThat(outboxDeadLetterQueue.getQueueSize()).isZero();
            });

        final var result = assertThat(outputDestination.receive(Duration.ofSeconds(1).toMillis(), "engineEvents"))
            .isNotNull()
            .actual();
        assertThat(result.getPayload()).isEqualTo("outbox-payload".getBytes());
        assertThat(result.getHeaders()).doesNotContainKey(OUTBOX_DESTINATION);
    }

    @Test
    @DisplayName(
        "Should rollback transaction and route DestinationResolutionException to outbox dead letter when OUTBOX_DESTINATION is missing"
    )
    void shouldRouteErrorToDeadLetterChannelWhenDestinationHeaderIsMissing() {
        Message<?> invalidMessage = MessageBuilder.withPayload("outbox-payload".getBytes()).build();

        auditProducerQueue.send(invalidMessage);

        await().untilAsserted(() -> {
            verify(streamBridge, never()).send(anyString(), any(Message.class));
            assertThat(auditProducerQueue.getQueueSize()).isZero();
            assertThat(outboxDeadLetterQueue.getQueueSize()).isEqualTo(1);
        });

        final var dlqMessage = outboxDeadLetterQueue.receive();

        assertThat(dlqMessage.getPayload()).isEqualTo("outbox-payload".getBytes());
        assertThat(dlqMessage.getHeaders())
            .containsEntry("x-error-class", "org.springframework.messaging.core.DestinationResolutionException")
            .containsEntry(
                "x-error-message",
                "Cannot resolve outboxDestination message header for auditProducer outbox"
            );
    }
}
