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
package org.activiti.cloud.common.messaging.function.router;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.CONNECTOR_TYPE;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_ANONYMOUS_INPUT;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.AUDIT_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.COMMAND_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.ENGINE_EVENTS_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.INTEGRATION_RESULT_TYPED_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.QUERY_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.REST_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.SCRIPT_RUNTIME_CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.cloud.common.messaging.config.test.FunctionRouterBindingConfigurationIT;
import org.activiti.cloud.common.messaging.config.test.TestBindingsChannels;
import org.activiti.cloud.common.messaging.config.test.TestBindingsChannelsConfiguration;
import org.activiti.cloud.common.messaging.function.router.FunctionRouterGateway.RouteResult;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "spring.cloud.stream.bindings.auditConsumer.destination=engine-events",
        "spring.cloud.stream.bindings.auditConsumer.group=audit",
        "spring.cloud.stream.bindings.queryConsumer.destination=engine-events",
        "spring.cloud.stream.bindings.queryConsumer.group=query",
        "spring.cloud.stream.bindings.engineEventsConsumer.destination=engine-events",
        "spring.cloud.stream.bindings.commandConsumer.destination=command-consumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.commandResults.destination=command-results",
        "spring.cloud.stream.bindings.scriptRuntimeConsumer.destination=script.EXECUTE",
        "spring.cloud.stream.bindings.scriptRuntimeConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.restConsumer.destination=rest.GET,rest.POST",
        "spring.cloud.stream.bindings.restConsumer.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.type=gateway",
        "activiti.cloud.messaging.function-router.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.consumer.concurrency=1",
        "activiti.cloud.messaging.function-router.routes.queryConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.auditConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.engineEventsConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.commandConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.scriptRuntimeConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.routes.restConsumer.enabled=true",
        "function-router.request-timeout=10s",
    }
)
@EnableTestBinder
@SpringBootApplication
@Import({ TestBindingsChannelsConfiguration.class })
class FunctionRouterGatewayIT {

    private static final AtomicReference<Message<?>> queryMessage = new AtomicReference<>();
    private static final AtomicReference<Message<?>> auditMessage = new AtomicReference<>();
    private static final AtomicReference<Message<?>> engineEventsMessage = new AtomicReference<>();
    private static final AtomicReference<Integer> auditRetries = new AtomicReference<>();
    private static final AtomicReference<String> connectorPayload = new AtomicReference<>();
    private static final AtomicReference<String> getPayload = new AtomicReference<>();
    private static final AtomicReference<String> postPayload = new AtomicReference<>();
    private static final AtomicReference<FunctionRouterBindingConfigurationIT.TypedPayload> receivedTypedPayload =
        new AtomicReference<>();

    @Autowired
    private TestBindingsChannels channels;

    @Autowired
    private FunctionRouterGateway functionRouterGateway;

    @Autowired
    private MessageChannel functionRouterGatewayInputChannel;

    @Autowired
    private BarrierMessageHandler barrierHandler;

    @Autowired
    private Supplier<String> functionRouterCorrelationHeaderAttribute;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private FunctionRouterDestinationsProvider functionRouterDestinationsProvider;

    @MockitoSpyBean
    private Consumer<ErrorMessage> functionRouterConsumerErrorHandler;

    @MockitoBean
    private MessageRecoverer republishMessageRecoverer;

    @Autowired
    private FunctionBindingConfiguration.BindingResolver bindingResolver;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean
        @FunctionBinding(input = QUERY_CONSUMER)
        public Consumer<Message<?>> queryConsumerHandler() {
            return message -> {
                queryMessage.set(message);
            };
        }

        @Bean
        @FunctionBinding(input = AUDIT_CONSUMER)
        public Consumer<Message<?>> auditConsumerHandler() {
            return message -> {
                auditMessage.set(message);
            };
        }

        @Bean
        @FunctionBinding(input = ENGINE_EVENTS_CONSUMER)
        public Consumer<Message<?>> engineEventsConsumerHandler() {
            return message -> {
                if (auditRetries.getAndSet(auditRetries.get() + 1) < 3) {
                    throw new RuntimeException("test error");
                }

                engineEventsMessage.set(message);
            };
        }

        @Bean
        @FunctionBinding(input = COMMAND_CONSUMER, output = TestBindingsChannels.COMMAND_RESULTS)
        public Function<Message<?>, Message<?>> commandProcessorHandler(TestBindingsChannels channels) {
            return message -> {
                AssertionsForClassTypes.assertThat(message).isNotNull();
                Message<?> outMessage = MessageBuilder.withPayload(message.getPayload())
                    .setHeader("type", "Test Send")
                    .build();
                channels.auditProducer().send(outMessage);
                return MessageBuilder.withPayload(message.getPayload()).setHeader("type", "Test Reply").build();
            };
        }

        @Bean
        @ConnectorBinding(input = SCRIPT_RUNTIME_CONSUMER, condition = "true")
        public ConsumerConnector<String> scriptRuntimeExecutor() {
            return message -> {
                connectorPayload.set(message);
            };
        }

        @Bean
        @ConnectorBinding(input = REST_CONSUMER, connectorType = "${FOOBAR:rest.GET}", condition = "true")
        public ConsumerConnector<String> restConsumerGetHandler() {
            return message -> {
                getPayload.set(message);
            };
        }

        @Bean
        @ConnectorBinding(input = REST_CONSUMER, connectorType = "rest.POST", condition = "true")
        public ConsumerConnector<String> restConsumerPostHandler() {
            return message -> {
                postPayload.set(message);
            };
        }

        @Bean
        @FunctionBinding(input = INTEGRATION_RESULT_TYPED_CONSUMER)
        public Consumer<
            Message<FunctionRouterBindingConfigurationIT.TypedPayload>
        > integrationResultTypedConsumerHandler() {
            return message -> receivedTypedPayload.set(message.getPayload());
        }
    }

    @BeforeEach
    public void setUp() {
        queryMessage.set(null);
        auditMessage.set(null);
        connectorPayload.set(null);
        getPayload.set(null);
        postPayload.set(null);
        receivedTypedPayload.set(null);
        auditRetries.set(0);
        output.clear();
    }

    @Test
    void contextLoads() {}

    @Test
    void functionRouterDestinationsProvider() {
        assertThat(functionRouterDestinationsProvider.apply(FUNCTION_ROUTER_INPUT)).containsOnly(
            "engine-events",
            "command-consumer",
            "script.EXECUTE",
            "rest.GET",
            "rest.POST"
        );

        assertThat(functionRouterDestinationsProvider.apply(FUNCTION_ROUTER_ANONYMOUS_INPUT)).containsOnly(
            "engine-events"
        );
    }

    @Test
    void functionRouterCorrelationHeaderAttribute() {
        assertThat(functionRouterCorrelationHeaderAttribute.get()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void testFunctionBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("Test")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader("type", "Test Consumer")
            .setHeader(FUNCTION_DESTINATION, "command-consumer")
            .build();

        // when
        channels.commandConsumer().send(message);

        // then
        await().untilAsserted(() -> {
            Message<?> outputMessage = output.receive(
                1000,
                bindingResolver.getBindingDestination(TestBindingsChannels.COMMAND_RESULTS)
            );
            AssertionsForClassTypes.assertThat(outputMessage).isNotNull();
            AssertionsForClassTypes.assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo(
                "Test Reply"
            );
        });

        // then
        await().untilAsserted(() -> {
            Message<?> outputMessage = output.receive(
                1000,
                bindingResolver.getBindingDestination(TestBindingsChannels.AUDIT_PRODUCER)
            );

            AssertionsForClassTypes.assertThat(outputMessage).isNotNull();
            AssertionsForClassTypes.assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo(
                "Test Send"
            );
        });
    }

    @Test
    void testConnectorBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("run_test();")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader(FUNCTION_DESTINATION, "script.EXECUTE")
            .build();

        // when
        inputDestination.send(message, "script.EXECUTE");

        // then
        await().untilAsserted(() -> {
            assertThat(connectorPayload.get()).isNotNull().isEqualTo("run_test();");
        });
    }

    @Test
    void testConnectorTypeBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("run_test();")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader(CONNECTOR_TYPE, "script.EXECUTE")
            .build();

        // when
        inputDestination.send(message, "script.EXECUTE");

        // then
        await().untilAsserted(() -> {
            assertThat(connectorPayload.get()).isNotNull().isEqualTo("run_test();");
        });
    }

    @Test
    void functionRouterGateway() {
        final var request = MessageBuilder.withPayload("buz")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader("spring.cloud.function.destination", "engine-events")
            .build();

        ExceptionCaptor<RuntimeException> exceptionCaptor = new ExceptionCaptor<>();
        doAnswer(exceptionCaptor).when(functionRouterConsumerErrorHandler).accept(any(ErrorMessage.class));

        inputDestination.send(request, "engine-events");

        // then
        await().untilAsserted(() -> {
            AssertionsForClassTypes.assertThat(queryMessage.get())
                .isNotNull()
                .extracting(msg -> msg.getHeaders().get("spring.cloud.function.definition", String.class))
                .isEqualTo("queryConsumerHandler_registration");
            AssertionsForClassTypes.assertThat(auditMessage.get())
                .isNotNull()
                .extracting(msg -> msg.getHeaders().get("spring.cloud.function.definition", String.class))
                .isEqualTo("auditConsumerHandler_registration");
            AssertionsForClassTypes.assertThat(engineEventsMessage.get())
                .isNotNull()
                .extracting(msg -> msg.getHeaders().get("spring.cloud.function.definition", String.class))
                .isEqualTo("engineEventsConsumerHandler_registration");
        });

        AssertionsForClassTypes.assertThat(auditRetries.get()).isEqualTo(4);
    }

    @Test
    void testGetConnectorBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("GET http://localhost:8080")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader(FUNCTION_DESTINATION, "rest.GET")
            .setHeader("connectorType", "rest.GET")
            .build();

        // when
        inputDestination.send(message, "rest.GET");

        // then
        await().untilAsserted(() -> {
            AssertionsForClassTypes.assertThat(getPayload.get()).isNotNull().isEqualTo("GET http://localhost:8080");
            AssertionsForClassTypes.assertThat(postPayload.get()).isNull();
        });
    }

    @Test
    void testPostConnectorBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("POST http://localhost:8080")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader(FUNCTION_DESTINATION, "rest.POST")
            .setHeader("connectorType", "rest.POST")
            .build();

        // when
        inputDestination.send(message, "rest.GET");

        // then
        await().untilAsserted(() -> {
            assertThat(postPayload.get()).isNotNull().isEqualTo("POST http://localhost:8080");
            assertThat(getPayload.get()).isNull();
        });
    }

    @Test
    void testConnectorBindingsAmqpHeaders() {
        // given
        Message<String> message = MessageBuilder.withPayload("run_test();")
            .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
            .setHeader(AmqpHeaders.RECEIVED_EXCHANGE, "script.EXECUTE")
            .build();

        // when
        inputDestination.send(message, "script.EXECUTE");

        // then
        await().untilAsserted(() -> {
            assertThat(connectorPayload.get()).isNotNull().isEqualTo("run_test();");
        });
    }

    @Test
    void testConnectorBindingsAmqpHeadersWithPrefix() {
        withRabbitMqPrefix("myapp.", prefix -> {
            // given
            Message<String> message = MessageBuilder.withPayload("run_test();")
                .setHeader(AmqpHeaders.MESSAGE_ID, UUID.randomUUID().toString())
                .setHeader(AmqpHeaders.RECEIVED_EXCHANGE, prefix.concat("script.EXECUTE"))
                .build();

            // when
            inputDestination.send(message, "script.EXECUTE");

            // then
            await().untilAsserted(() -> {
                assertThat(connectorPayload.get()).isNotNull().isEqualTo("run_test();");
            });
        });
    }

    @Test
    @Disabled
    void testBarrierReleasesWhenTriggerMatches() throws Exception {
        String correlationId = "12345";
        final var message = MessageBuilder.withPayload("Main Payload")
            .setHeader(functionRouterCorrelationHeaderAttribute.get(), correlationId)
            .build();

        // 1. Send original message on a separate thread (it will block)
        Future<RouteResult> resultFuture = functionRouterGateway.applyAsync(message);

        // Small pause to ensure the main thread enters the barrier
        Thread.sleep(200);

        // 2. Send trigger message with matching correlation ID to release the barrier
        Message<? extends MessageGroup> triggerMessage = MessageBuilder.withPayload(
            new SimpleMessageGroup(List.of(MessageBuilder.withPayload("Test Payload").build()), UUID.randomUUID())
        )
            .setHeader(CORRELATION_ID, correlationId)
            .build();

        barrierHandler.trigger(triggerMessage);

        // 3. Verify the released message contains the combined result
        final var releasedMessage = resultFuture.get(2, TimeUnit.SECONDS);

        assertThat(releasedMessage)
            .isNotNull()
            .satisfies(
                payload -> assertThat(payload.request().getPayload()).isEqualTo("Main Payload"),
                payload ->
                    assertThat(payload.results())
                        .asInstanceOf(InstanceOfAssertFactories.type(MessageGroup.class))
                        .extracting(MessageGroup::getMessages)
                        .satisfies(messages ->
                            assertThat(messages).allMatch(m -> "Test Payload".equals(m.getPayload()))
                        )
            );
    }

    @Test
    @Disabled
    void testBarrierReleasesWhenTriggerMatches2() throws Exception {
        final var correlationId = UUID.nameUUIDFromBytes("54321".getBytes());
        final var destination = "events";
        final var message = MessageBuilder.withPayload("buz").setHeader(AmqpHeaders.MESSAGE_ID, correlationId).build();

        CompletableFuture<RouteResult> resultFuture = functionRouterGateway.forwardToAsync(destination, message);

        // 3. Verify the released message contains the combined result
        final var routeResult = resultFuture.get(10, TimeUnit.SECONDS);

        assertThat(routeResult)
            .isNotNull()
            .satisfies(
                payload -> assertThat(payload.request().getPayload()).isEqualTo("buz"),
                payload ->
                    assertThat(payload.results())
                        .extracting(MessageGroup::getMessages)
                        .asInstanceOf(InstanceOfAssertFactories.collection(Message.class))
                        .hasSize(3)
                        .filteredOn(ErrorMessage.class::isInstance)
                        .hasSize(1)
                        .first()
                        .asInstanceOf(InstanceOfAssertFactories.type(ErrorMessage.class))
                        .satisfies(error ->
                            assertThat(error.getPayload()).isInstanceOf(RuntimeException.class).hasMessage("Buz error")
                        )
            );

        CompletableFuture<RouteResult> secondFuture = functionRouterGateway.forwardToAsync(destination, message);

        // 3. Verify the released message contains the combined result
        final var duplicateRouteResult = secondFuture.get(10, TimeUnit.SECONDS);

        assertThat(duplicateRouteResult)
            .isNotNull()
            .satisfies(
                payload -> assertThat(payload.request().getPayload()).isEqualTo("buz"),
                payload -> assertThat(payload.hasErrors()).isTrue(),
                payload ->
                    assertThat(payload.getErrors())
                        .hasSize(3)
                        .filteredOn(ErrorMessage.class::isInstance)
                        .hasSize(3)
                        .extracting(Message::getPayload)
                        .satisfies(errors ->
                            assertThat(errors).filteredOn(MessageRejectedException.class::isInstance).hasSize(2)
                        )
                        .satisfies(errors ->
                            assertThat(errors)
                                .filteredOn(Predicate.not(MessageRejectedException.class::isInstance))
                                .hasSize(1)
                                .first()
                                .satisfies(error -> assertThat(error).hasMessage("Buz error"))
                        )
            );
    }

    public static class ExceptionCaptor<T extends Throwable> implements Answer<Object> {

        private T result = null;

        public T getException() {
            return result;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            // Call the actual method and store the result
            try {
                return invocation.callRealMethod();
            } catch (Throwable e) {
                result = (T) e;

                throw e;
            }
        }
    }

    void withRabbitMqPrefix(String prefix, Consumer<String> runnable) {
        final var current = messagingProperties.getRabbitmq().getPrefix();

        try {
            messagingProperties.getRabbitmq().setPrefix(prefix);

            runnable.accept(prefix);
        } finally {
            messagingProperties.getRabbitmq().setPrefix(current);
        }
    }
}
