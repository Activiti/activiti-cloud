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

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
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
public class ConditionalFunctionBindingConfigurationIT {

    private static final String FUNCTION_NAME_A = "auditConsumerHandlerA";
    private static final String FUNCTION_NAME_B = "auditConsumerHandlerB";
    private static final String FUNCTION_NAME_C = "auditProcessorHandler";

    private static Message<?> consumedMessage = null;

    @Autowired
    private FunctionBindingPropertySource functionBindingPropertySource;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean(FUNCTION_NAME_A)
        @ConditionalFunctionBinding(input = TestBindingsChannels.AUDIT_CONSUMER, condition = "headers['type']=='TestAuditConsumerA'")
        public Consumer<Message<?>> auditConsumerHandlerA() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerA");
                consumedMessage = message;
            };
        }

        @Bean(FUNCTION_NAME_B)
        @ConditionalFunctionBinding(input = TestBindingsChannels.AUDIT_CONSUMER, condition = "headers['type']=='TestAuditConsumerB'")
        public Consumer<Message<?>> auditConsumerHandlerB() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerB");
                consumedMessage = message;
            };
        }

        @Bean(FUNCTION_NAME_C)
        @ConditionalFunctionBinding(input = TestBindingsChannels.AUDIT_CONSUMER, output=TestBindingsChannels.COMMAND_RESULTS, condition = "headers['type']=='TestAuditConsumerC'")
        public Function<Message<?>, Message<?>> auditProcessorHandler() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerC");
                return MessageBuilder.withPayload("TestReply").setHeader("type", "TestAuditConsumerReply").build();
            };
        }
    }

    @BeforeEach
    public void setUp(){
        consumedMessage = null;
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
        assertThat(functions).contains(FUNCTION_NAME_A + "Gateway", FUNCTION_NAME_B + "Gateway", FUNCTION_NAME_C + "Gateway");
    }

    @Test
    public void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_A + "Gateway")).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_B + "Gateway")).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_C + "Gateway")).isNotNull();
    }

    @Test
    public void testRoutingFunctionInvokesConsumer() throws InterruptedException {
        // given
        Message<String> message = MessageBuilder.withPayload("TestA").setHeader("type", "TestAuditConsumerB").build();

        // when
        input.send(message, "engineEvents");

        // then
        assertThat(consumedMessage).isNotNull()
            .extracting(Message::getHeaders)
            .extracting(headers -> headers.get("type", String.class))
            .isNotNull()
            .isEqualTo("TestAuditConsumerB");
    }

    @Test
    public void testResolvesFunctionAndReplies() throws InterruptedException {
        // given
        Message<String> message = MessageBuilder.withPayload("TestC").setHeader("type", "TestAuditConsumerC").build();

        // when
        input.send(message, "engineEvents");

        // then
        Message<byte[]> reply = output.receive(10000, "commandResults_foo");
        assertThat(reply).isNotNull()
            .extracting(Message::getPayload)
            .isNotNull()
            .isEqualTo("TestReply".getBytes(StandardCharsets.UTF_8));
    }
}
