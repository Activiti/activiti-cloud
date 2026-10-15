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
package org.activiti.cloud.common.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.activiti.cloud.common.messaging.config.StatefulRetryContainerCustomizerConfiguration.ListenerExecutionFailedExceptionInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;
import org.springframework.amqp.rabbit.retry.NewMessageIdentifier;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.util.ReflectionTestUtils;

class StatefulRetryContainerCustomizerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(
        StatefulRetryContainerCustomizerConfiguration.class
    );

    @Nested
    class InterceptorBeanTests {

        @Test
        void shouldLoadInterceptorWithDefaultMaxRetries() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(StatefulRetryOperationsInterceptor.class);
                StatefulRetryOperationsInterceptor interceptor = context.getBean(
                    StatefulRetryOperationsInterceptor.class
                );

                // Extract and assert internal fields to verify maxRetries setup via reflection
                Object retryPolicy = ReflectionTestUtils.getField(interceptor, "retryPolicy");
                Object backOff = ReflectionTestUtils.getField(retryPolicy, "backOff");
                Object maxAttempts = ReflectionTestUtils.getField(backOff, "maxAttempts");

                // maxRetries(3) translates to 3 attempts in RetryTemplate
                assertThat(maxAttempts).isEqualTo(3L);
            });
        }

        @Test
        void shouldLoadInterceptorWithCustomMaxRetriesProperty() {
            contextRunner.withPropertyValues("stateful-retry-operations-interceptor.max-retries=5").run(context -> {
                StatefulRetryOperationsInterceptor interceptor = context.getBean(
                    StatefulRetryOperationsInterceptor.class
                );
                Object retryPolicy = ReflectionTestUtils.getField(interceptor, "retryPolicy");
                Object backOff = ReflectionTestUtils.getField(retryPolicy, "backOff");
                Object maxAttempts = ReflectionTestUtils.getField(backOff, "maxAttempts");

                assertThat(maxAttempts).isEqualTo(5L);
            });
        }
    }

    @Nested
    class ContainerCustomizerTests {

        @Test
        void shouldApplyAdviceChainAndPropertiesToAbstractContainer() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ListenerContainerCustomizer.class);
                @SuppressWarnings("unchecked")
                ListenerContainerCustomizer<MessageListenerContainer> customizer = context.getBean(
                    ListenerContainerCustomizer.class
                );

                // Use an implementation instance of AbstractMessageListenerContainer
                AbstractMessageListenerContainer mockContainer = mock(AbstractMessageListenerContainer.class);
                StatefulRetryOperationsInterceptor statefulRetryOperationsInterceptor = context.getBean(
                    StatefulRetryOperationsInterceptor.class
                );

                ListenerExecutionFailedExceptionInterceptor listenerExecutionFailedExceptionInterceptor =
                    context.getBean(ListenerExecutionFailedExceptionInterceptor.class);

                customizer.configure(mockContainer, "test-destination", "test-group");

                // Verify that structural changes are propagated down to the container wrapper
                verify(mockContainer).setStatefulRetryFatalWithNullMessageId(false);
                verify(mockContainer).setAdviceChain(
                    statefulRetryOperationsInterceptor,
                    listenerExecutionFailedExceptionInterceptor
                );
            });
        }
    }

    @Nested
    class ErrorMessageHandlerTests {

        @Test
        void shouldUnwrapMessageHandlingExceptionToRuntimeMostSpecificCause() {
            contextRunner.run(context -> {
                @SuppressWarnings("unchecked")
                Consumer<ErrorMessage> handler = context.getBean("statefulRetryErrorMessageHandler", Consumer.class);

                IllegalArgumentException rootCause = new IllegalArgumentException("Root validation failure");
                MessageHandlingException exceptionWrapper = new MessageHandlingException(
                    mock(org.springframework.messaging.Message.class),
                    "Failed handling",
                    rootCause
                );
                ErrorMessage errorMessage = new ErrorMessage(exceptionWrapper);

                assertThatThrownBy(() -> handler.accept(errorMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(rootCause);
            });
        }

        @Test
        void shouldIgnoreNonMessageHandlingExceptionPayloads() {
            contextRunner.run(context -> {
                @SuppressWarnings("unchecked")
                Consumer<ErrorMessage> handler = context.getBean("statefulRetryErrorMessageHandler", Consumer.class);

                ErrorMessage errorMessage = new ErrorMessage(new IllegalArgumentException("Other error"));

                // Assert no exception is rethrown because it is not an instance of MessageHandlingException
                assertThatCode(() -> handler.accept(errorMessage)).doesNotThrowAnyException();
            });
        }
    }

    @Nested
    class MessageKeyGeneratorTests {

        @Test
        void shouldReturnMessageIdIfPresent() {
            contextRunner.run(context -> {
                MessageKeyGenerator keyGenerator = context.getBean("messageKeyGenerator", MessageKeyGenerator.class);

                MessageProperties properties = new MessageProperties();
                properties.setMessageId("unique-id-123");
                Message message = new Message(new byte[0], properties);

                Object key = keyGenerator.getKey(message);
                assertThat(key).isEqualTo("unique-id-123");
                assertThat(properties.isFinalRetryForMessageWithNoId()).isFalse();
            });
        }

        @Test
        void shouldSetFinalRetryFlagIfMessageIdNullAndRedelivered() {
            contextRunner.run(context -> {
                MessageKeyGenerator keyGenerator = context.getBean("messageKeyGenerator", MessageKeyGenerator.class);

                MessageProperties properties = new MessageProperties();
                properties.setMessageId(null);
                properties.setRedelivered(true);
                Message message = new Message(new byte[0], properties);

                Object key = keyGenerator.getKey(message);
                assertThat(key).isNull();
                assertThat(properties.isFinalRetryForMessageWithNoId()).isTrue();
            });
        }
    }

    @Nested
    class NewMessageIdentifierTests {

        @Test
        void shouldIdentifyAsNewMessageIfRedeliveredIsFalse() {
            contextRunner.run(context -> {
                NewMessageIdentifier identifier = context.getBean("newMessageIdentifier", NewMessageIdentifier.class);

                MessageProperties properties = new MessageProperties();
                properties.setRedelivered(false);
                Message message = new Message(new byte[0], properties);

                assertThat(identifier.isNew(message)).isTrue();
            });
        }

        @Test
        void shouldNotIdentifyAsNewMessageIfRedeliveredIsTrue() {
            contextRunner.run(context -> {
                NewMessageIdentifier identifier = context.getBean("newMessageIdentifier", NewMessageIdentifier.class);

                MessageProperties properties = new MessageProperties();
                properties.setRedelivered(true);
                Message message = new Message(new byte[0], properties);

                assertThat(identifier.isNew(message)).isFalse();
            });
        }
    }

    @Test
    void customMethodInterceptorShouldPassThroughSuccessfulInvocations() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            ListenerContainerCustomizer<SimpleMessageListenerContainer> customizer = context.getBean(
                ListenerContainerCustomizer.class
            );

            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            customizer.configure(container, "destination-topic", "consumer-group");

            MethodInterceptor listenerExecutionFailedExceptionInterceptor = context.getBean(
                ListenerExecutionFailedExceptionInterceptor.class
            );

            // Verify normal processing yields the true target method return value
            MethodInvocation mockSuccessInvocation = mock(MethodInvocation.class);
            when(mockSuccessInvocation.proceed()).thenReturn("SUCCESS_PAYLOAD");

            Object result = listenerExecutionFailedExceptionInterceptor.invoke(mockSuccessInvocation);
            assertThat(result).isEqualTo("SUCCESS_PAYLOAD");
        });
    }

    @Test
    void customMethodInterceptorShouldUnwrapAndRethrowMostSpecificCause() {
        contextRunner.run(context -> {
            // 1. Resolve customizer and required beans
            @SuppressWarnings("unchecked")
            ListenerContainerCustomizer<SimpleMessageListenerContainer> customizer = context.getBean(
                ListenerContainerCustomizer.class
            );

            // 2. Instantiate a concrete container implementation
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

            // 3. Apply customizer setup to populate the container's advice chain
            customizer.configure(container, "destination-topic", "consumer-group");

            // 4. Retrieve the customized advice chain array from the concrete container
            MethodInterceptor listenerExecutionFailedExceptionInterceptor = context.getBean(
                ListenerExecutionFailedExceptionInterceptor.class
            );

            // 5. Construct a nested exception tree
            NullPointerException rootCause = new NullPointerException("Database field missing");
            ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException(
                "Listener execution processing failed",
                new RuntimeException("Intermediate wrapper exception", rootCause),
                new Message(new byte[0])
            );

            // 6. Mock the AOP MethodInvocation context to simulate an underlying listener failure
            MethodInvocation mockInvocation = mock(MethodInvocation.class);
            when(mockInvocation.proceed()).thenThrow(listenerException);

            // 7. Verify interceptor runtime behavior matches custom expectations
            assertThatThrownBy(() -> listenerExecutionFailedExceptionInterceptor.invoke(mockInvocation))
                .isInstanceOf(ListenerExecutionFailedException.class)
                .hasMessage("Listener execution processing failed")
                // Verifies that NestedExceptionUtils correctly pinpointed the leaf NullPointerException cause
                .hasRootCause(rootCause)
                .satisfies(thrown -> {
                    ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) thrown;
                    assertThat(lefe.getCause()).isSameAs(rootCause);
                    assertThat(lefe.getFailedMessage()).isNull();
                });
        });
    }
}
