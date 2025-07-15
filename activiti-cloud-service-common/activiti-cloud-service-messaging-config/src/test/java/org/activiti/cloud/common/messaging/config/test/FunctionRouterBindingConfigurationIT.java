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
package org.activiti.cloud.common.messaging.config.test;

import static org.activiti.cloud.common.messaging.config.AbstractFunctionalBindingConfiguration.getInBinding;
import static org.activiti.cloud.common.messaging.config.InputBindingConfiguration.INPUT_BINDING;
import static org.activiti.cloud.common.messaging.config.OutputBindingConfiguration.OUTPUT_BINDING;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.AUDIT_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.COMMAND_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.INTEGRATION_REQUESTS;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.QUERY_CONSUMER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration.BindingResolver;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "spring.cloud.stream.bindings.auditProducer.destination=engine-events",
        "spring.cloud.stream.bindings.auditProducer.producer.required-groups=query,audit",
        "spring.cloud.stream.bindings.commandConsumer.destination=command-consumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.commandResults.destination=command-results",
        "spring.cloud.stream.bindings.auditConsumer.destination=engine-events",
        "spring.cloud.stream.bindings.queryConsumer.destination=engine-events",
        "spring.cloud.stream.bindings.queryConsumer.group=query",
        "spring.cloud.stream.bindings.integrationRequests.destination=integration-requests",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.consumer.concurrency=2",
        "activiti.cloud.messaging.function-router.bindings.commandConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.bindings.queryConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.bindings.auditConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.bindings.integrationRequests.enabled=true",
    }
)
@EnableTestBinder
@Import({ TestBindingsChannelsConfiguration.class })
public class FunctionRouterBindingConfigurationIT {

    private static final String FUNCTION_HANDLER_NAME = "queryConsumerHandler";
    private static final String FUNCTION_PROCESSOR_NAME = "commandProcessorHandler";
    private static final String FUNCTION_AUDIT_SUPPLIER_NAME = "auditProducer" + OUTPUT_BINDING;
    private static final String FUNCTION_COMMAND_SUPPLIER_NAME = "commandResults" + OUTPUT_BINDING;
    private static final String FUNCTION_COMMAND_CONSUMER_NAME = "commandConsumer" + INPUT_BINDING;
    private static final String FUNCTION_AUDIT_CONSUMER_NAME = "auditConsumer" + INPUT_BINDING;
    private static final String FUNCTION_QUERY_CONSUMER_NAME = "queryConsumer" + INPUT_BINDING;
    private static final String FUNCTION_INTEGRATION_REQUESTS_NAME = "integrationRequests" + INPUT_BINDING;

    private static final AtomicReference<Message<?>> queryMessage = new AtomicReference<>();
    private static final AtomicReference<Message<?>> auditMessage = new AtomicReference<>();

    @Autowired
    private TestBindingsChannels channels;

    @Autowired
    private FunctionBindingPropertySource functionBindingPropertySource;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private StreamFunctionProperties streamFunctionProperties;

    @Autowired
    private BindingResolver bindingResolver;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private Environment environment;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean(FUNCTION_HANDLER_NAME)
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
        @FunctionBinding(input = AUDIT_CONSUMER)
        public Consumer<Message<?>> errorConsumerHandler() {
            return message -> {
                throw new MessageHandlingException(message, "test error");
            };
        }

        @Bean(FUNCTION_PROCESSOR_NAME)
        @FunctionBinding(input = COMMAND_CONSUMER, output = TestBindingsChannels.COMMAND_RESULTS)
        public Function<Message<?>, Message<?>> commandProcessorHandler(TestBindingsChannels channels) {
            return message -> {
                assertThat(message).isNotNull();
                Message<?> outMessage = MessageBuilder
                    .withPayload(message.getPayload())
                    .setHeader("type", "Test Send")
                    .build();
                channels.auditProducer().send(outMessage);
                return MessageBuilder.withPayload(message.getPayload()).setHeader("type", "Test Reply").build();
            };
        }
    }

    @BeforeEach
    public void setUp() {
        queryMessage.set(null);
        auditMessage.set(null);
        output.clear();
    }

    @Test
    void bindingResolver() {
        assertThat(bindingResolver.getBindingDestination("integrationRequests")).isEqualTo("integration-requests");
        assertThat(bindingResolver.getBindingDestination("commandResults")).isEqualTo("command-results");
        assertThat(bindingResolver.getBindingDestination("fooBar")).isEqualTo("fooBar");
    }

    @Test
    void producerGroups() {
        assertThat(bindingServiceProperties.getProducerProperties("auditProducer").getRequiredGroups())
            .containsOnly("audit");
    }

    @Test
    void testFunctionDefinitions() {
        // given
        String functionDefinitions = (String) functionBindingPropertySource.getProperty(
            FunctionBindingPropertySource.SPRING_CLOUD_FUNCTION_DEFINITION
        );

        assertThat(functionDefinitions).isNotNull();

        String[] functions = functionDefinitions.split(";");

        // then
        assertThat(functions).containsOnly("");
    }

    @Test
    void functionRouterBinding() {
        // when
        var functionRouterInput = bindingServiceProperties.getBindingProperties("functionRouterInput");

        // then
        assertThat(functionRouterInput)
            .isNotNull()
            .extracting(BindingProperties::getDestination)
            .satisfies(destination ->
                assertThat(List.of(destination.split(",")))
                    .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                    .containsOnly("engine-events", "command-consumer", "integration-requests")
            );

        assertThat(functionRouterInput).isNotNull().extracting(BindingProperties::getGroup).isEqualTo("bar");
    }

    @Test
    void bindingServiceProperties() {
        // when
        var bindings = bindingServiceProperties.getBindings();

        // then
        assertThat(
            bindings
                .entrySet()
                .stream()
                .filter(entry ->
                    entry.getValue().getConsumer() == null || entry.getValue().getConsumer().isAutoStartup()
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        )
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, BindingProperties.class))
            .containsOnlyKeys("auditProducer", "commandResults", "functionRouterInput", "integrationResults");
    }

    @Test
    void testOutputBindingsDefinitions() {
        // then
        assertThat(context.getBean(TestBindingsChannels.AUDIT_PRODUCER, MessageChannel.class)).isNotNull();
        assertThat(bindingServiceProperties.getOutputBindings()).contains(FUNCTION_AUDIT_SUPPLIER_NAME);
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_AUDIT_SUPPLIER_NAME))
            .isEqualTo(Arrays.asList(TestBindingsChannels.AUDIT_PRODUCER));

        assertThat(context.getBean(TestBindingsChannels.COMMAND_RESULTS, MessageChannel.class)).isNotNull();
        assertThat(bindingServiceProperties.getOutputBindings()).contains(FUNCTION_COMMAND_SUPPLIER_NAME);
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_COMMAND_SUPPLIER_NAME))
            .isEqualTo(Arrays.asList(TestBindingsChannels.COMMAND_RESULTS));
    }

    @Test
    void testInputBindingsDefinitions() {
        Assertions.assertThat(context.getBean(COMMAND_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions
            .assertThat(bindingServiceProperties.getInputBindings())
            .doesNotContain(FUNCTION_COMMAND_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_COMMAND_CONSUMER_NAME)))
            .isNull();

        Assertions.assertThat(context.getBean(AUDIT_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).doesNotContain(FUNCTION_AUDIT_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_AUDIT_CONSUMER_NAME)))
            .isNull();

        Assertions.assertThat(context.getBean(QUERY_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).doesNotContain(FUNCTION_QUERY_CONSUMER_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_QUERY_CONSUMER_NAME)))
            .isNull();

        Assertions.assertThat(context.getBean(INTEGRATION_REQUESTS, MessageChannel.class)).isNotNull();
        Assertions
            .assertThat(bindingServiceProperties.getInputBindings())
            .doesNotContain(FUNCTION_INTEGRATION_REQUESTS_NAME);
        Assertions
            .assertThat(streamFunctionProperties.getBindings().get(getInBinding(FUNCTION_INTEGRATION_REQUESTS_NAME)))
            .isNull();
    }

    @Test
    void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_HANDLER_NAME + REGISTRATION_NAME_SUFFIX)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_PROCESSOR_NAME + REGISTRATION_NAME_SUFFIX)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_AUDIT_SUPPLIER_NAME)).isNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_COMMAND_SUPPLIER_NAME)).isNull();
    }

    @Test
    void testStreamBindings() {
        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_AUDIT_SUPPLIER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_AUDIT_SUPPLIER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains(TestBindingsChannels.AUDIT_PRODUCER));

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_COMMAND_SUPPLIER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_COMMAND_SUPPLIER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains(TestBindingsChannels.COMMAND_RESULTS));
    }

    @Test
    void testConsumerBindings() {
        // given
        Message<String> message = MessageBuilder
            .withPayload("Test")
            .setHeader("type", "Test Consumer")
            .setHeader("spring.cloud.function.destination", "engine-events")
            .build();

        // when
        input.send(message, "engine-events");

        // then
        await()
            .untilAsserted(() -> {
                assertThat(queryMessage.get())
                    .isNotNull()
                    .extracting(msg -> msg.getHeaders().get("spring.cloud.function.definition", String.class))
                    .isEqualTo("queryConsumerHandler_registration");
                assertThat(auditMessage.get())
                    .isNotNull()
                    .extracting(msg -> msg.getHeaders().get("spring.cloud.function.definition", String.class))
                    .isEqualTo("auditConsumerHandler_registration");
            });
    }

    @Test
    void testFunctionBindings() {
        // given
        Message<String> message = MessageBuilder
            .withPayload("Test")
            .setHeader("type", "Test Consumer")
            .setHeader("spring.cloud.function.destination", "command-consumer")
            .build();

        // when
        channels.commandConsumer().send(message);

        // then
        await()
            .untilAsserted(() -> {
                Message<?> outputMessage = output.receive(
                    1000,
                    bindingResolver.getBindingDestination(TestBindingsChannels.COMMAND_RESULTS)
                );
                assertThat(outputMessage).isNotNull();
                assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo("Test Reply");
            });

        // then
        await()
            .untilAsserted(() -> {
                Message<?> outputMessage = output.receive(
                    1000,
                    bindingResolver.getBindingDestination(TestBindingsChannels.AUDIT_PRODUCER)
                );

                assertThat(outputMessage).isNotNull();
                assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo("Test Send");
            });
    }
}
