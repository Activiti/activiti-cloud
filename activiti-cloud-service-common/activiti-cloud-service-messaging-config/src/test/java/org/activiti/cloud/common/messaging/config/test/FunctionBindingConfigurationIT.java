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

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration.BindingResolver;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.AUDIT_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.COMMAND_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.QUERY_CONSUMER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {
    "activiti.cloud.application.name=foo",
    "spring.application.name=bar",

    "activiti.cloud.messaging.destination-transformers-enabled=false",

    "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
    "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",

    "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
    "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
    "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",

    "spring.cloud.stream.bindings.commandResults.destination=commandResults"
})
@Import({TestChannelBinderConfiguration.class, TestBindingsChannelsConfiguration.class})
public class FunctionBindingConfigurationIT {

    private static final String FUNCTION_HANDLER_NAME = "queryConsumerHandler";
    private static final String FUNCTION_PROCESSOR_NAME = "commandProcessorHandler";
    private static final String FUNCTION_AUDIT_SUPPLIER_NAME = "auditProducerSupplier";
    private static final String FUNCTION_COMMAND_SUPPLIER_NAME = "commandResultsSupplier";

    private static Message<?> consumerMessage = null;

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

    @TestConfiguration
    static class ApplicationConfig {

        @Bean(FUNCTION_HANDLER_NAME)
        @FunctionBinding(input = QUERY_CONSUMER)
        public Consumer<Message<?>> queryConsumerHandler() {
            return message -> {
                consumerMessage = message;
            };
        }

        @Bean(FUNCTION_PROCESSOR_NAME)
        @FunctionBinding(input = COMMAND_CONSUMER, output = TestBindingsChannels.COMMAND_RESULTS)
        public Function<Message<?>, Message<?>> commandProcessorHandler(TestBindingsChannels channels) {
            return message -> {
                assertThat(message).isNotNull();
                Message outMessage = MessageBuilder.withPayload(message.getPayload()).setHeader("type", "Test Send").build();
                channels.auditProducer().send(outMessage);
                return MessageBuilder.withPayload(message.getPayload()).setHeader("type", "Test Reply").build();
            };
        }

    }

    @BeforeEach
    public void setUp() {
        consumerMessage = null;
        output.clear();
    }

    @Test
    public void testFunctionDefinitions() {

        // given
        String functionDefinitions = (String) functionBindingPropertySource
            .getProperty(FunctionBindingPropertySource.SPRING_CLOUD_FUNCTION_DEFINITION);

        assertThat(functionDefinitions).isNotNull();

        String[] functions = functionDefinitions.split(";");

        // then
        assertThat(functions).contains(FUNCTION_HANDLER_NAME, FUNCTION_PROCESSOR_NAME);
        assertThat(functions).doesNotContain(FUNCTION_AUDIT_SUPPLIER_NAME, FUNCTION_COMMAND_SUPPLIER_NAME);
    }

    @Test
    public void testOutputBindingsDefinitions() {
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
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).contains("commandConsumerBinding");
        Assertions.assertThat(streamFunctionProperties.getBindings().get("commandConsumerBinding-in-0")).isEqualTo(COMMAND_CONSUMER);

        Assertions.assertThat(context.getBean(AUDIT_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).contains("auditConsumerBinding");
        Assertions.assertThat(streamFunctionProperties.getBindings().get("auditConsumerBinding-in-0")).isEqualTo(AUDIT_CONSUMER);

        Assertions.assertThat(context.getBean(QUERY_CONSUMER, MessageChannel.class)).isNotNull();
        Assertions.assertThat(bindingServiceProperties.getInputBindings()).contains("queryConsumerBinding");
        Assertions.assertThat(streamFunctionProperties.getBindings().get("queryConsumerBinding-in-0")).isEqualTo(QUERY_CONSUMER);

    }

    @Test
    public void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_HANDLER_NAME)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_PROCESSOR_NAME)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_AUDIT_SUPPLIER_NAME)).isNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_COMMAND_SUPPLIER_NAME)).isNull();
    }

    @Test
    public void testStreamBindings() {
        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains(QUERY_CONSUMER));
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains(COMMAND_CONSUMER));
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains(TestBindingsChannels.COMMAND_RESULTS));

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
    public void testConsumerBindings() {
        // given
        Message<String> message = MessageBuilder.withPayload("Test").setHeader("type", "Test Consumer").build();

        // when
        input.send(message, "engineEvents");

        // then
        assertThat(consumerMessage).isNotNull();
        assertThat(consumerMessage.getHeaders().get("type", String.class)).isEqualTo("Test Consumer");
    }

    @Test
    public void testFunctionBindings() throws InterruptedException {
        // given
        Message<String> message = MessageBuilder.withPayload("Test").setHeader("type", "Test Consumer").build();

        // when
        channels.commandConsumer().send(message);

        // then
        Awaitility.await().untilAsserted(() -> {
            Message<?> outputMessage = output.receive(10000, bindingResolver.apply(TestBindingsChannels.COMMAND_RESULTS));
            assertThat(outputMessage).isNotNull();
            assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo("Test Reply");

            assertThat(consumerMessage).isNotNull();
            assertThat(consumerMessage.getHeaders().get("type", String.class)).isEqualTo("Test Send");
        });
    }
}
