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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionBinding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(properties = {
    "activiti.cloud.application.name=foo",
    "spring.application.name=bar",
    "POD_NAMESPACE=baz",
    "ENV_NAME=quix",
    "DEST_SEPARATOR=.",

    "activiti.cloud.messaging.destination-separator=${DEST_SEPARATOR}",
    "activiti.cloud.messaging.destination-prefix=${ENV_NAME}${DEST_SEPARATOR}${POD_NAMESPACE}",
    "activiti.cloud.messaging.destination-transformers-enabled=true",

    "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
    "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
    "spring.cloud.stream.bindings.messageConnectorOutput.destination=commandConsumer",
    "spring.cloud.stream.bindings.messageConnectorOutput.group=${spring.application.name}",

    "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
    "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
    "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",

    // override destination name
    "activiti.cloud.messaging.destinations.engineEvents.name=engine-events",

    "spring.cloud.stream.bindings.[camel-connector.INVOKE].destination=camel-connector.INVOKE",
    "spring.cloud.stream.bindings.camelConnectorConsumer.destination=camel-connector.INVOKE",
    "activiti.cloud.messaging.destinations.[camel-connector.INVOKE].name=${ACT_CAMEL_CONNECTOR_INVOKE_DEST:camel_connector_invoke}",

    "spring.cloud.stream.bindings.asyncExecutorJobsInput.destination=asyncExecutorJobs",
    "spring.cloud.stream.bindings.asyncExecutorJobsOutput.destination=asyncExecutorJobs",

    "spring.cloud.stream.bindings.messageEventsOutput.destination=messageEvents",
    "spring.cloud.stream.bindings.messageConnectorInput.destination=messageEvents",
    "activiti.cloud.messaging.destinations.messageEvents.name=message-events",

    "spring.cloud.stream.bindings.commandResults.destination=commandResults",
    "activiti.cloud.messaging.destinations.commandResults.name=command-results",
    "activiti.cloud.messaging.destinations.commandResults.prefix=bar",
    "activiti.cloud.messaging.destinations.commandResults.separator=_"})
@Import(TestChannelBinderConfiguration.class)
public class ConditionalFunctionBindingConfigurationIT {

    private static final String FUNCTION_NAME_A = "auditConsumerHandlerA";
    private static final String FUNCTION_NAME_B = "auditConsumerHandlerB";
    private static final String FUNCTION_NAME_C = "auditProcessorHandler";

    private static final String AUDIT_CONSUMER_ROUTER = "auditConsumerRouter";

    @Autowired
    private MessageChannel auditConsumer;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean
        public MessageChannel auditConsumer() {
            return MessageChannels.publishSubscribe("auditConsumer").get();
        }

        @Bean
        public MessageChannel auditProducer() {
            return MessageChannels.direct("auditProducer").get();
        }

        @Bean(FUNCTION_NAME_A)
        @ConditionalFunctionBinding(input = "auditConsumer", condition = "headers['type']=='TestAuditConsumerA'")
        public Consumer<Message<?>> auditConsumerHandlerA() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerA");
            };
        }

        @Bean(FUNCTION_NAME_B)
        @ConditionalFunctionBinding(input = "auditConsumer", condition = "headers['type']=='TestAuditConsumerB'")
        public Consumer<Message<?>> auditConsumerHandlerB() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerB");
            };
        }

        @Bean(FUNCTION_NAME_C)
        @ConditionalFunctionBinding(input = "auditConsumer", output="auditProducer", condition = "headers['type']=='TestAuditConsumerC'")
        public Function<Message<?>, Message<?>> auditProcessorHandler() {
            return message -> {
                assertThat(message.getHeaders().get("type", String.class)).isEqualTo("TestAuditConsumerC");
                return MessageBuilder.withPayload(message.getPayload()).setHeader("type", "TestAuditConsumerReply").build();
            };
        }
    }

    @Autowired
    private FunctionBindingPropertySource functionBindingPropertySource;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Test
    public void testFunctionDefinitions() {

        // given
        String functionDefinitions = (String) functionBindingPropertySource
            .getProperty(FunctionBindingPropertySource.SPRING_CLOUD_FUNCTION_DEFINITION);

        assertThat(functionDefinitions).isNotNull();

        String[] functions = functionDefinitions.split(";");

        // then
        assertThat(functions).contains(FUNCTION_NAME_A, FUNCTION_NAME_B, FUNCTION_NAME_C);
        assertThat(functions).contains(AUDIT_CONSUMER_ROUTER);
    }

    @Test
    public void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_A)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_B)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_C)).isNotNull();

        assertThat(functionRegistry.<Object>lookup(AUDIT_CONSUMER_ROUTER)).isNotNull();
    }

    @Test
    public void testRouterCallbacks(){
        assertThat(applicationContext.containsBeanDefinition(AUDIT_CONSUMER_ROUTER + "Callback")).isTrue();
    }

    @Test
    public void testFunctionRoutingCallbacksResolvesFunction() {
        // given
        FunctionInvocationWrapper auditConsumerRouter = functionRegistry.<FunctionInvocationWrapper>lookup(AUDIT_CONSUMER_ROUTER);

        Message<String> messageA = MessageBuilder.withPayload("TestA").setHeader("type", "TestAuditConsumerA").build();
        Message<String> messageB = MessageBuilder.withPayload("TestB").setHeader("type", "TestAuditConsumerB").build();
        Message<String> messageZ = MessageBuilder.withPayload("TestC").setHeader("type", "TestAuditConsumerZ").build();

        // then
        assertThat(auditConsumerRouter.apply(messageA)).isNull();
        assertThat(auditConsumerRouter.apply(messageB)).isNull();
        assertThrows(IllegalStateException.class, () -> auditConsumerRouter.apply(messageZ));
    }

    @Test
    public void testFunctionRoutingCallbacksResolvesFunctionAndAllowsReply() throws InterruptedException {
        // given
        FunctionInvocationWrapper auditConsumerRouter = functionRegistry.<FunctionInvocationWrapper>lookup(AUDIT_CONSUMER_ROUTER);

        Message<String> message = MessageBuilder.withPayload("TestA").setHeader("type", "TestAuditConsumerC").build();

        // when
        auditConsumer.send(message);

        // then
        Message<byte[]> reply = output.receive();
        assertThat(reply).isNotNull()
            .extracting(Message::getHeaders)
            .extracting(headers -> headers.get("type", String.class))
            .isNotNull()
            .isEqualTo("TestAuditConsumerReply");
    }
}
