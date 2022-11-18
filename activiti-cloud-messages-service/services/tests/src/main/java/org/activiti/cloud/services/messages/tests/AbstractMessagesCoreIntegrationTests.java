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
package org.activiti.cloud.services.messages.tests;

import static java.util.Collections.singletonMap;
import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.messaging.MessageHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.activiti.api.process.model.builders.MessageEventPayloadBuilder;
import org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents;
import org.activiti.api.process.model.events.MessageDefinitionEvent.MessageDefinitionEvents;
import org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.messages.core.aggregator.MessageConnectorAggregator;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorSource;
import org.activiti.cloud.services.messages.core.config.MessageAggregatorProperties;
import org.activiti.cloud.services.messages.core.controlbus.ControlBusGateway;
import org.activiti.cloud.services.messages.core.correlation.Correlations;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tests for the Message Connector Aggregator Processor.
 *
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.application.name=rb",
        "activiti.cloud.application.name=default-app",
        "spring.cloud.stream.bindings.messageConnectorInput.content-type=application/json",
        "spring.cloud.stream.bindings.messageConnectorOutput.content-type=application/json"
    }
)
@DirtiesContext
@Import({ AbstractMessagesCoreIntegrationTests.TestConfigurationContext.class })
public abstract class AbstractMessagesCoreIntegrationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessagesCoreIntegrationTests.class);

    protected ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    protected MessageConnectorProcessor channels;

    @Autowired
    protected MessageCollector collector;

    @Autowired
    protected MessageGroupStore messageGroupStore;

    @Autowired
    protected MessageConnectorAggregator aggregatingMessageHandler;

    @Autowired
    protected QueueChannel errorQueue;

    @Autowired
    protected QueueChannel discardQueue;

    @Autowired
    protected ControlBusGateway controlBus;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected MessageAggregatorProperties messageAggregatorProperties;

    @Autowired
    protected AbstractMessageChannel messageConnectorOutput;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Value("${activiti.cloud.application.name}")
    protected String activitiCloudApplicationName;

    @Value("${spring.application.name}")
    protected String springApplicationName;

    @TestConfiguration
    static class TestConfigurationContext {

        @Bean
        @BridgeFrom("errorChannel")
        MessageChannel errorQueue() {
            return MessageChannels.queue().get();
        }

        @Bean
        @BridgeFrom("discardChannel")
        MessageChannel discardQueue() {
            return MessageChannels.queue().get();
        }
    }

    @Test
    public void shouldConfigureInputHeadersToRemove() {
        assertThat(messageAggregatorProperties.getInputHeadersToRemove()).contains("kafka_consumer");
    }

    @Test
    public void shouldConfigureHeaderChannelsTimeToLiveExpression() {
        assertThat(messageAggregatorProperties.getHeaderChannelsTimeToLiveExpression())
            .contains("headers['headerChannelsTTL']?:60000");
    }

    @Test
    @Timeout(20000)
    public void shouldProcessMessageEventsConcurrently() throws InterruptedException, JsonProcessingException {
        // given
        String messageEventName = "start";
        Integer count = 100;

        Message<?> startMessage = startMessageDeployedEvent(messageEventName);
        String correlationId = correlationId(startMessage);
        removeMessageGroup(correlationId);

        send(startMessage);

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);

        // when
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch sent = new CountDownLatch(count);

        ExecutorService exec = Executors.newSingleThreadExecutor();

        IntStream.range(0, count).forEach(i -> sendAsync(messageSentEvent(messageEventName), start, sent, exec));
        start.countDown();

        try {
            sent.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // then
        IntStream
            .range(0, count)
            .mapToObj(i -> Try.call(() -> poll(1, TimeUnit.SECONDS)))
            .forEach(out -> assertThat(out).isNotNull());

        exec.shutdownNow();

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);

        assertThat(peek()).isNull();
    }

    @Test
    @Timeout(20000)
    public void shouldProcessMessageEventsConcurrentlyInReversedOrder()
        throws InterruptedException, JsonProcessingException {
        // given
        String messageEventName = "start";
        Integer count = 100;
        Message<?> startMessage = startMessageDeployedEvent(messageEventName);
        String correlationId = correlationId(startMessage);

        removeMessageGroup(correlationId);

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch sent = new CountDownLatch(count);

        ExecutorService exec = Executors.newSingleThreadExecutor();

        IntStream.range(0, count).forEach(i -> sendAsync(messageSentEvent(messageEventName), start, sent, exec));
        start.countDown();

        try {
            sent.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(messageGroup(correlationId).getMessages()).hasSize(count);

        // when
        send(startMessage);

        // then
        IntStream
            .range(0, count)
            .mapToObj(i -> Try.call(() -> poll(3, TimeUnit.SECONDS)))
            .forEach(out -> assertThat(out).isNotNull());

        exec.shutdownNow();

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);

        assertThat(peek()).isNull();
    }

    @Test
    public void testStartMessageBeforeSent() throws Exception {
        // given
        String messageName = "start1";
        Message<?> startMessage = startMessageDeployedEvent(messageName);
        String correlationId = correlationId(startMessage);
        removeMessageGroup(correlationId);

        send(startMessage);

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);

        // when
        send(messageSentEvent(messageName, null, "sent1"));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(peek()).isNull();

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "variables")
            .contains("start1", singletonMap("key", "sent1"));

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("name")
            .containsOnly(messageName);
        // when
        send(messageSentEvent(messageName, null, "sent2"));

        // then
        out = poll(0, TimeUnit.SECONDS);

        assertThat(peek()).isNull();

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "variables")
            .contains("start1", singletonMap("key", "sent2"));

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("name")
            .containsOnly(messageName);
    }

    @Test
    public void testStartMessageAfterSent() throws Exception {
        // given
        String messageName = "start2";
        Message<?> messageSentEvent = messageSentEvent(messageName, null, "sent1");
        String correlationId = correlationId(messageSentEvent);

        messageGroupStore.removeMessageGroup(correlationId);

        send(messageSentEvent);

        // when
        send(startMessageDeployedEvent(messageName));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(peek()).isNull();

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "businessKey", "variables")
            .contains(messageName, "sent1", singletonMap("key", "sent1"));

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("name")
            .containsOnly("start2");

        // when
        send(messageSentEvent(messageName, null, "sent2"));

        // then
        out = poll(0, TimeUnit.SECONDS);

        assertThat(peek()).isNull();

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "businessKey", "variables")
            .contains(messageName, "sent2", singletonMap("key", "sent2"));

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("name")
            .containsOnly("start2");
    }

    @Test
    public void testSentMessagesWithBuffer() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = "1";
        Message<?> messageSentEvent = messageSentEvent(messageName, correlationKey, "sent1");
        String correlationId = correlationId(messageSentEvent);

        messageGroupStore.removeMessageGroup(correlationId);

        send(messageSentEvent);

        // when
        send(messageWaitingEvent(messageName, correlationKey, "waiting1"));
        send(messageWaitingEvent(messageName, correlationKey, "waiting2"));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("variables")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("key", "sent1");

        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(2)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .containsOnly(singletonMap("key", "waiting1"), singletonMap("key", "waiting2"));

        // when
        send(messageReceivedEvent(messageName, correlationKey));

        // then
        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .containsOnly(singletonMap("key", "waiting2"));

        // when
        send(messageSentEvent(messageName, correlationKey, "sent2"));

        // then
        out = poll(1, TimeUnit.SECONDS);

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("variables")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("key", "sent2");

        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .containsOnly(singletonMap("key", "waiting2"));

        // then
        send(messageReceivedEvent(messageName, correlationKey));

        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages()).isEmpty();
    }

    @Test
    public void testReceiveMessagePayload() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = "1";
        String businessKey = "businessKey";
        Message<?> messageSentEvent = messageSentEvent(messageName, correlationKey, businessKey);
        String correlationId = correlationId(messageSentEvent);

        messageGroupStore.removeMessageGroup(correlationId);

        send(messageSentEvent);

        // when
        send(messageWaitingEvent(messageName, correlationKey, businessKey));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "correlationKey", "variables")
            .contains(messageName, correlationKey, singletonMap("key", businessKey));

        assertThat(peek()).isNull();

        // cleanup
        messageGroupStore.removeMessageGroup(correlationId);
    }

    @Test
    public void testStartMessagePayload() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = null;
        String businessKey = "businessKey";
        Message<?> startMessageDeployedEvent = startMessageDeployedEvent(messageName);
        String correlationId = correlationId(startMessageDeployedEvent);

        messageGroupStore.removeMessageGroup(correlationId);

        send(startMessageDeployedEvent);

        // when
        send(messageSentEvent(messageName, correlationKey, businessKey));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("name", "businessKey", "variables")
            .contains(messageName, businessKey, singletonMap("key", businessKey));

        assertThat(peek()).isNull();

        // cleanup
        messageGroupStore.removeMessageGroup(correlationId);
    }

    @Test
    public void testSentMessagesWithBufferInDifferentOrder() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = "1";
        String correlationId = correlationId(messageWaitingEvent(messageName, correlationKey));

        messageGroupStore.removeMessageGroup(correlationId);

        send(messageSentEvent(messageName, correlationKey, "sent1"));
        send(messageSentEvent(messageName, correlationKey, "sent2"));

        // when
        send(messageWaitingEvent(messageName, correlationKey, "waiting1"));

        // then
        Message<?> out = poll(0, TimeUnit.SECONDS);

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("variables")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("key", "sent1");

        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(2)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .contains(singletonMap("key", "sent2"), singletonMap("key", "waiting1"));
        // when
        send(messageReceivedEvent(messageName, correlationKey, "received1"));

        // then
        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .containsOnly(singletonMap("key", "sent2"));
        // when
        send(messageWaitingEvent(messageName, correlationKey, "waiting2"));

        // then
        out = poll(0, TimeUnit.SECONDS);

        assertThat(peek()).isNull();

        assertThat(out)
            .isNotNull()
            .extracting(Message::getPayload)
            .extracting("variables")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("key", "sent2");

        assertThat(messageGroup(correlationId).getMessages())
            .hasSize(1)
            .extracting(Message::getPayload)
            .asList()
            .extracting("variables")
            .containsOnly(singletonMap("key", "waiting2"));

        // when
        send(messageReceivedEvent(messageName, correlationKey, "received2"));

        // then
        assertThat(peek()).isNull();

        assertThat(messageGroup(correlationId).getMessages()).isEmpty();
    }

    @Test
    public void testSubscriptionCancelled() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = "1";
        Message<?> subscriptionCancelled = subscriptionCancelledEvent(messageName, correlationKey);
        String groupName = correlationId(subscriptionCancelled);

        messageGroupStore.removeMessageGroup(groupName);

        send(messageWaitingEvent(messageName, correlationKey));
        send(messageWaitingEvent(messageName, correlationKey));

        assertThat(messageGroup(groupName).getMessages()).hasSize(2);

        // when
        send(subscriptionCancelled);

        // then
        assertThat(peek()).isNull();

        assertThat(messageGroup(groupName).getMessages()).isEmpty();
    }

    @Test
    public void testIdempotentMessageInterceptor() throws Exception {
        // given
        String messageName = "message";
        String correlationKey = "1";
        Message<MessageEventPayload> waitingMessage = messageWaitingEvent(messageName, correlationKey);
        String correlationId = correlationId(waitingMessage);

        messageGroupStore.removeMessageGroup(correlationId);

        // when
        send(waitingMessage);
        send(waitingMessage);

        // then
        assertThat(peek()).isNull();

        assertThat(errorQueue.receive(0)).isNotNull();

        assertThat(messageGroup(correlationId).getMessages()).isNotNull().hasSize(1);
        // given
        Message<MessageEventPayload> receivedMessage = messageReceivedEvent(messageName, correlationKey);

        // when
        send(receivedMessage);
        send(receivedMessage);

        // then
        assertThat(peek()).isNull();

        assertThat(errorQueue.receive(0)).isNotNull();

        assertThat(messageGroup(correlationId).getMessages()).hasSize(0);
    }

    @Test
    public void testMessageFilterDiscardChannel() throws Exception {
        // given
        Message<String> invalidMessage = MessageBuilder
            .withPayload("message")
            .setHeader(CONTENT_TYPE, "text/plain")
            .build();
        // when
        this.channels.input().send(invalidMessage);

        // then
        assertThat(peek()).isNull();

        Message<?> out = discardQueue.receive(0);

        assertThat(out.getPayload()).isEqualTo("message");
    }

    @Test
    public void testInvalidMessagePayloadDiscardChannel() throws Exception {
        // given
        Message<String> invalidMessage = MessageBuilder
            .withPayload("payload")
            .setHeader(CONTENT_TYPE, "text/plain")
            .setHeader(MESSAGE_EVENT_TYPE, MessageEvents.MESSAGE_SENT.name())
            .build();
        // when
        Throwable thrown = catchThrowable(() -> {
            this.channels.input().send(invalidMessage);
        });

        // then
        assertThat(peek()).isNull();

        Message<?> out = errorQueue.receive(0);

        assertThat(out).isNull();
        assertThat(thrown).isInstanceOf(MessageTransformationException.class);
    }

    @Test
    public void testControlBusStartStopComponents() throws Exception {
        String messageName = "test";

        this.controlBus.send("@aggregator.stop()");

        Throwable thrown = catchThrowable(() -> {
            send(messageSentEvent(messageName, null, "error"));
        });

        assertThat(thrown).isInstanceOf(MessageDeliveryException.class);

        this.controlBus.send("@aggregator.start()");
    }

    @Test
    public void testTransactionException() throws Exception {
        // given
        String messageName = "start1";
        Message<?> startMessage = startMessageDeployedEvent(messageName);
        String correlationId = correlationId(startMessage);
        removeMessageGroup(correlationId);

        send(startMessage);

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);

        // when
        ChannelInterceptor assertionInterceptor = new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                throw new RuntimeException("transaction failed");
            }
        };

        messageConnectorOutput.addInterceptor(assertionInterceptor);

        Throwable thrown = catchThrowable(() -> {
            send(messageSentEvent(messageName, null, "error"));
        });

        messageConnectorOutput.removeInterceptor(assertionInterceptor);

        assertThat(messageGroup(correlationId).getMessages()).hasSize(1);
        assertThat(thrown).isInstanceOf(MessageDeliveryException.class);
    }

    protected MessageBuilder<MessageEventPayload> messageBuilder(String messageName) {
        return messageBuilder(messageName, null, null);
    }

    protected MessageBuilder<MessageEventPayload> messageBuilder(String messageName, String correlationKey) {
        return messageBuilder(messageName, correlationKey, null);
    }

    protected MessageBuilder<MessageEventPayload> messageBuilder(
        String messageName,
        String correlationKey,
        String businessKey
    ) {
        MessageEventPayload payload = MessageEventPayloadBuilder
            .messageEvent(messageName)
            .withCorrelationKey(correlationKey)
            .withBusinessKey(businessKey)
            .withVariables(Collections.singletonMap("key", businessKey))
            .build();
        return MessageBuilder
            .withPayload(payload)
            .setHeader(MESSAGE_EVENT_NAME, messageName)
            .setHeader(MESSAGE_EVENT_CORRELATION_KEY, correlationKey)
            .setHeader(MESSAGE_EVENT_ID, UUID.randomUUID())
            .setHeader(APP_NAME, activitiCloudApplicationName)
            .setHeader(
                MESSAGE_EVENT_OUTPUT_DESTINATION,
                bindingServiceProperties.getBindingDestination(MessageConnectorSource.OUTPUT)
            )
            .setHeader(SERVICE_FULL_NAME, springApplicationName);
    }

    protected Message<MessageEventPayload> startMessageDeployedEvent(String messageName) {
        return messageBuilder(messageName, null)
            .setHeader(MESSAGE_EVENT_TYPE, MessageDefinitionEvents.START_MESSAGE_DEPLOYED.name())
            .build();
    }

    protected Message<MessageEventPayload> messageSentEvent(String messageName) {
        return messageSentEvent(messageName, null);
    }

    protected Message<MessageEventPayload> messageSentEvent(String messageName, String correlationKey) {
        return messageSentEvent(messageName, correlationKey, null);
    }

    protected Message<MessageEventPayload> messageSentEvent(
        String messageName,
        String correlationKey,
        String businessKey
    ) {
        return messageBuilder(messageName, correlationKey, businessKey)
            .setHeader(MESSAGE_EVENT_TYPE, MessageEvents.MESSAGE_SENT.name())
            .build();
    }

    protected Message<MessageEventPayload> messageWaitingEvent(String messageName) {
        return messageWaitingEvent(messageName, null);
    }

    protected Message<MessageEventPayload> messageWaitingEvent(
        String messageName,
        String correlationKey,
        String businessKey
    ) {
        return messageBuilder(messageName, correlationKey, businessKey)
            .setHeader(MESSAGE_EVENT_TYPE, MessageEvents.MESSAGE_WAITING.name())
            .build();
    }

    protected Message<MessageEventPayload> messageWaitingEvent(String messageName, String correlationKey) {
        return messageBuilder(messageName, correlationKey)
            .setHeader(MESSAGE_EVENT_TYPE, MessageEvents.MESSAGE_WAITING.name())
            .build();
    }

    protected Message<MessageEventPayload> messageReceivedEvent(String messageName, String correlationKey) {
        return messageReceivedEvent(messageName, correlationKey, null);
    }

    protected Message<MessageEventPayload> messageReceivedEvent(
        String messageName,
        String correlationKey,
        String businessKey
    ) {
        return messageBuilder(messageName, correlationKey, businessKey)
            .setHeader(MESSAGE_EVENT_TYPE, MessageEvents.MESSAGE_RECEIVED.name())
            .build();
    }

    protected Message<MessageEventPayload> subscriptionCancelledEvent(String messageName, String correlationKey) {
        return messageBuilder(messageName, correlationKey)
            .setHeader(MESSAGE_EVENT_TYPE, MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED.name())
            .build();
    }

    protected void send(Message<?> message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message.getPayload());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        this.channels.input().send(MessageBuilder.withPayload(json).copyHeaders(message.getHeaders()).build());
    }

    @SuppressWarnings("unchecked")
    protected <T> Message<T> poll(long timeout, TimeUnit unit) throws InterruptedException {
        Message<T> message = (Message<T>) this.collector.forChannel(this.channels.output()).poll(timeout, unit);

        return (Message<T>) Optional
            .ofNullable(message)
            .map(it -> MessageBuilder.withPayload(messageEventPayload(it)).copyHeaders(it.getHeaders()).build())
            .orElse(null);
    }

    @SuppressWarnings("unchecked")
    protected <T> Message<T> peek() {
        return (Message<T>) this.collector.forChannel(this.channels.output()).peek();
    }

    protected MessageGroup messageGroup(String groupName) {
        return aggregatingMessageHandler.getMessageStore().getMessageGroup(groupName);
    }

    protected String correlationId(Message<?> message) {
        return Correlations.getCorrelationId(message);
    }

    protected void removeMessageGroup(String correlationId) {
        messageGroupStore.removeMessageGroup(correlationId);
    }

    protected void sendAsync(
        final Message<?> message,
        final CountDownLatch start,
        final CountDownLatch sent,
        ExecutorService exec
    ) {
        exec.execute(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Try.run(() -> send(message));

            sent.countDown();
        });
    }

    static class Try {

        @FunctionalInterface
        public interface ExceptionWrapper<E> {
            E wrap(Exception e);
        }

        @FunctionalInterface
        public interface RunnableExceptionWrapper {
            void run() throws Exception;
        }

        public static <T> T call(Callable<T> callable) throws RuntimeException {
            return call(callable, RuntimeException::new);
        }

        public static void run(RunnableExceptionWrapper runnable) {
            try {
                runnable.run();
            } catch (Exception e) {
                sneakyThrow(e);
            }
        }

        public static <T, E extends Throwable> T call(Callable<T> callable, ExceptionWrapper<E> wrapper) throws E {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.wrap(e);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
            throw (T) t;
        }
    }

    private Object messageEventPayload(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof String) {
            try {
                return objectMapper.readValue((String) payload, MessageEventPayload.class);
            } catch (JsonProcessingException e) {
                LOGGER.warn(
                    "The payload {} cannot be converted to MessageEventPayload, so it is returned as is: {}",
                    payload,
                    e
                );
                return payload;
            }
        }
        return payload;
    }
}
