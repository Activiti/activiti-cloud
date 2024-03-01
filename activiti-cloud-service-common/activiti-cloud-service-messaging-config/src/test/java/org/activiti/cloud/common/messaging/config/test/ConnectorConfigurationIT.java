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

import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.AUDIT_CONSUMER;
import static org.activiti.cloud.common.messaging.config.test.TestBindingsChannels.COMMAND_RESULTS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration.BindingResolver;
import org.activiti.cloud.common.messaging.config.FunctionBindingPropertySource;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "application.min.version=1",
        "application.max.version=17",
        "spring.cloud.stream.function.autodetect=false",
        "activiti.cloud.messaging.destination-transformers-enabled=false",
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
        "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
        "spring.cloud.stream.default.error-handler-definition=myErrorHandler",
    }
)
@Import({ TestChannelBinderConfiguration.class, TestBindingsChannelsConfiguration.class })
public class ConnectorConfigurationIT {

    private static final String FUNCTION_NAME_A = "auditConsumerHandlerA";
    private static final String FUNCTION_NAME_B = "auditConsumerHandlerB";
    private static final String FUNCTION_NAME_C = "auditProcessorHandler";
    private static final String FUNCTION_NAME_ERROR = "connectorTestMyErrorHandler";

    private static final String FUNCTION_NAME_D = "auditProcessorVersionHandler";
    public static final String MY_ERROR_HANDLER = "myErrorHandler";

    @Autowired
    private StandardEvaluationContext evaluationContext;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private FunctionBindingPropertySource functionBindingPropertySource;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private BindingResolver bindingResolver;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @SpyBean
    private MyErrorHandler myErrorHandler;

    @Value("${application.min.version}")
    private String minVersion;

    @Value("${application.max.version}")
    private String maxVersion;

    private String condition = AnnotationUtils.getDefaultValue(ConnectorBinding.class, "condition").toString();

    private ExpressionParser parser = new SpelExpressionParser();

    private String expression;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean(FUNCTION_NAME_A)
        @ConnectorBinding(input = AUDIT_CONSUMER, condition = "headers['type']=='TestAuditConsumerA'")
        public ConsumerConnector<?> auditConsumerHandlerA() {
            return payload -> {
                assertThat(payload).isNotNull().isEqualTo("TestA");
            };
        }

        @Bean(FUNCTION_NAME_B)
        @ConnectorBinding(input = AUDIT_CONSUMER, condition = "headers['type']=='TestAuditConsumerB'")
        public ConsumerConnector<?> auditConsumerHandlerB() {
            return payload -> {
                assertThat(payload).isNotNull().isEqualTo("TestB");
            };
        }

        @Bean(FUNCTION_NAME_C)
        @ConnectorBinding(
            input = AUDIT_CONSUMER,
            output = COMMAND_RESULTS,
            condition = "headers['type']=='TestAuditConsumerC'"
        )
        public Connector<?, ?> auditProcessorHandler() {
            return payload -> {
                assertThat(payload).isNotNull().isEqualTo("TestC");
                return "TestReply";
            };
        }

        @Bean(FUNCTION_NAME_ERROR)
        @ConnectorBinding(
            input = AUDIT_CONSUMER,
            output = COMMAND_RESULTS,
            condition = "headers['type']=='myErrorHandler'"
        )
        public Connector<?, ?> connectorTestMyErrorHandler() {
            return payload -> {
                throw new IllegalArgumentException("Test Audit Consumer Error");
            };
        }

        @Bean(FUNCTION_NAME_D)
        @ConnectorBinding(input = AUDIT_CONSUMER, output = COMMAND_RESULTS)
        public Connector<?, ?> auditProcessorVersionHandler() {
            return payload -> "TestVersion";
        }

        @Bean
        public MyErrorHandler myErrorHandler() {
            return new MyErrorHandler();
        }
    }

    @BeforeEach
    public void setUp() {
        expression = resolveExpression(condition);
        output.clear();
    }

    @Test
    void defaultErrorHandlerDefinition() {
        assertThat(bindingServiceProperties.getBindingProperties(AUDIT_CONSUMER))
            .extracting(BindingProperties::getErrorHandlerDefinition)
            .isEqualTo(MY_ERROR_HANDLER);
    }

    @Test
    public void testFunctionDefinitions() {
        // given
        String functionDefinitions = (String) functionBindingPropertySource.getProperty(
            FunctionBindingPropertySource.SPRING_CLOUD_FUNCTION_DEFINITION
        );

        assertThat(functionDefinitions).isNotNull();

        String[] functions = functionDefinitions.split(";");

        // then
        assertThat(functions).isEqualTo(new String[] { "" });
    }

    @Test
    public void testFunctionRegistry() {
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_A + REGISTRATION_NAME_SUFFIX)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_B + REGISTRATION_NAME_SUFFIX)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_C + REGISTRATION_NAME_SUFFIX)).isNotNull();
        assertThat(functionRegistry.<Object>lookup(FUNCTION_NAME_D + REGISTRATION_NAME_SUFFIX)).isNotNull();
    }

    @Test
    public void testApplicationVersionsSet() {
        Assertions.assertThat(minVersion).isEqualTo("1");
        Assertions.assertThat(maxVersion).isEqualTo("17");
    }

    @Test
    public void testConditionDefaultAttributeValue() {
        // when
        Object condition = AnnotationUtils.getDefaultValue(ConnectorBinding.class, "condition");
        // then
        Assertions
            .assertThat(condition)
            .isEqualTo(
                "T(Integer).valueOf(headers['appVersion']) >= ${application.min.version} and T(Integer).valueOf(headers['appVersion']) <= ${application.max.version}"
            );
    }

    @Test
    public void testShouldResolveConditionExpression() {
        // given
        String expression = resolveExpression(condition);

        // then
        Assertions
            .assertThat(expression)
            .isEqualTo(
                "T(Integer).valueOf(headers['appVersion']) >= 1 and T(Integer).valueOf(headers['appVersion']) <= 17"
            );
    }

    @Test
    public void testShouldPassMessageWithValidMinAppVersionCondition() {
        // given
        Message<?> message = MessageBuilder.withPayload(Map.of()).setHeader("appVersion", minVersion).build();

        // when
        Boolean result = getExpressionValue(message);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testShouldPassMessageWithValidMaxAppVersionCondition() {
        // given
        Message<?> message = MessageBuilder.withPayload(Map.of()).setHeader("appVersion", maxVersion).build();
        // when
        Boolean result = getExpressionValue(message);

        // then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testShouldDiscardMessageWithInvalidAppVersionCondition() {
        // given
        Message<?> message = MessageBuilder
            .withPayload(Map.of())
            .setHeader("appVersion", "20")
            .setHeader("resultDestination", "commandResults")
            .build();
        // when
        Boolean result = getExpressionValue(message);

        // then
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testShouldHandleMessageWithValidAppVersion() {
        // given
        Message<?> message = MessageBuilder
            .withPayload(Map.of())
            .setHeader("appVersion", "6")
            .setHeader("resultDestination", "commandResults")
            .build();

        // when
        input.send(message, "engineEvents");

        // then
        Message<byte[]> reply = output.receive(10000, bindingResolver.apply(COMMAND_RESULTS));
        assertThat(reply)
            .isNotNull()
            .extracting(Message::getPayload)
            .isNotNull()
            .isEqualTo("TestVersion".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testShouldDiscardMessageWithInValidAppVersion() {
        // given
        Message<?> message = MessageBuilder
            .withPayload(Map.of())
            .setHeader("appVersion", "20")
            .setHeader("resultDestination", "commandResults")
            .build();
        // when
        input.send(message, "engineEvents");

        // then
        Message<byte[]> reply = output.receive(2000, bindingResolver.apply(COMMAND_RESULTS));
        assertThat(reply).isNull();
    }

    @Test
    public void testConnectorsResolvesFunctionAndReplies() {
        // given
        Message<String> message = MessageBuilder
            .withPayload("TestC")
            .setHeader("type", "TestAuditConsumerC")
            .setHeader("appVersion", "1")
            .setHeader("resultDestination", "commandResults")
            .build();
        // when
        input.send(message, "engineEvents");

        // then
        Message<byte[]> reply = output.receive(10000, bindingResolver.apply(COMMAND_RESULTS));
        assertThat(reply)
            .isNotNull()
            .extracting(Message::getPayload)
            .isNotNull()
            .isEqualTo("TestReply".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testConnectorMyErrorHandler() {
        // given
        Message<String> message = MessageBuilder
            .withPayload("TestC")
            .setHeader("type", "myErrorHandler")
            .setHeader("appVersion", "1")
            .setHeader("resultDestination", "commandResults")
            .build();
        // when
        input.send(message, "engineEvents");

        await().untilAtomic(myErrorHandler.get(), Matchers.notNullValue());

        // then
        verify(myErrorHandler, times(1)).accept(any(ErrorMessage.class));

        assertThat(myErrorHandler.get())
            .extracting(AtomicReference::get)
            .extracting(ErrorMessage::getPayload)
            .extracting(Throwable::getCause)
            .extracting(Throwable::getMessage)
            .isEqualTo("Test Audit Consumer Error");
    }

    private String resolveExpression(String value) {
        BeanExpressionResolver resolver = this.applicationContext.getBeanFactory().getBeanExpressionResolver();
        BeanExpressionContext expressionContext = new BeanExpressionContext(applicationContext.getBeanFactory(), null);

        String resolvedValue = this.applicationContext.getBeanFactory().resolveEmbeddedValue(value);
        if (resolvedValue.startsWith("#{") && value.endsWith("}")) {
            resolvedValue = (String) resolver.evaluate(resolvedValue, expressionContext);
        }
        return resolvedValue;
    }

    private Boolean getExpressionValue(Message<?> message) {
        evaluationContext.setRootObject(message);

        return parser.parseExpression(expression).getValue(evaluationContext, Boolean.class);
    }

    static class MyErrorHandler implements Consumer<ErrorMessage>, Supplier<AtomicReference<ErrorMessage>> {

        private final AtomicReference<ErrorMessage> reference = new AtomicReference<>();

        @Override
        public void accept(ErrorMessage errorMessage) {
            reference.set(errorMessage);
        }

        @Override
        public AtomicReference<ErrorMessage> get() {
            return reference;
        }
    }
}
