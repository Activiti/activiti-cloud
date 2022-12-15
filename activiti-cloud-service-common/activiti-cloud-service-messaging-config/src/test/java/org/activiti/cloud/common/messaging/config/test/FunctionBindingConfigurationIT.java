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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration.BindingResolver;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

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
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean(FUNCTION_HANDLER_NAME)
        @FunctionBinding(input = TestBindingsChannels.QUERY_CONSUMER)
        public Consumer<Message<?>> queryConsumerHandler() {
            return message -> {
                consumerMessage = message;
            };
        }

        @Bean(FUNCTION_PROCESSOR_NAME)
        @FunctionBinding(input = TestBindingsChannels.COMMAND_CONSUMER, output = TestBindingsChannels.COMMAND_RESULTS)
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
        assertThat(functions).contains(FUNCTION_HANDLER_NAME, FUNCTION_PROCESSOR_NAME, FUNCTION_AUDIT_SUPPLIER_NAME, FUNCTION_COMMAND_SUPPLIER_NAME);
    }

    @Test
    public void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_HANDLER_NAME)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_PROCESSOR_NAME)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_AUDIT_SUPPLIER_NAME)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_COMMAND_SUPPLIER_NAME)).isNotNull();
    }

    @Test
    public void testStreamBindings() {
        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains("queryConsumer"));
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_HANDLER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains("commandConsumer"));
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_PROCESSOR_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains("commandResults_foo"));

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_AUDIT_SUPPLIER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_AUDIT_SUPPLIER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains("engineEvents"));

        assertThat(streamFunctionProperties.getInputBindings(FUNCTION_COMMAND_SUPPLIER_NAME))
            .matches(bindings -> bindings == null || bindings.isEmpty());
        assertThat(streamFunctionProperties.getOutputBindings(FUNCTION_COMMAND_SUPPLIER_NAME))
            .matches(bindings -> bindings.size() == 1 && bindings.contains("commandResults_foo"));
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

        Thread.sleep(1000);

        // then
        Message<?> outputMessage = output.receive(10000, bindingResolver.apply(TestBindingsChannels.COMMAND_RESULTS));
        assertThat(outputMessage).isNotNull();
        assertThat(outputMessage.getHeaders().get("type", String.class)).isEqualTo("Test Reply");

        assertThat(consumerMessage).isNotNull();
        assertThat(consumerMessage.getHeaders().get("type", String.class)).isEqualTo("Test Send");
    }
}
