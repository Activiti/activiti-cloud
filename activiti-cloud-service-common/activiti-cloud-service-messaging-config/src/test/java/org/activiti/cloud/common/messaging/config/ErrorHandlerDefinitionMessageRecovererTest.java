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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIllegalStateException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerDefinitionMessageRecovererTest {

    @Mock
    private MessageRecoverer messageRecoverer;

    @InjectMocks
    private ErrorHandlerDefinitionMessageRecoverer recoverer;

    @Test
    @DisplayName("Should successfully recover when recoverableExceptions is empty")
    void shouldRecoverWhenNoExceptionConfigured() {
        // given
        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        org.springframework.messaging.Message<?> springMessage = createSpringMessageWithAmqpSource(amqpMessage);
        Exception cause = new RuntimeException("Test cause");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // when
        recoverer.accept(errorMessage);

        // then
        verify(messageRecoverer).recover(amqpMessage, cause);
    }

    @Test
    @DisplayName("Should successfully recover when exception matches included (true) list")
    void shouldRecoverWhenExceptionIsIncluded() {
        // given
        recoverer.withRecoverableExceptions(
            Map.of(IllegalArgumentException.class, true, NullPointerException.class, false)
        );

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        org.springframework.messaging.Message<?> springMessage = createSpringMessageWithAmqpSource(amqpMessage);
        Exception cause = new IllegalArgumentException("Valid error");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // when
        recoverer.accept(errorMessage);

        // then
        verify(messageRecoverer).recover(amqpMessage, cause);
    }

    @Test
    @DisplayName("Should rethrow RuntimeException wrapping cause when exception is not included or excluded")
    void shouldThrowExceptionWhenNotRecoverableAndNotIgnored() {
        // given
        recoverer.withRecoverableExceptions(Map.of(IllegalArgumentException.class, true));

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        org.springframework.messaging.Message<?> springMessage = createSpringMessageWithAmqpSource(amqpMessage);
        Exception cause = new NullPointerException("Unconfigured error");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // when & Assert
        assertThatThrownBy(() -> recoverer.accept(errorMessage))
            .isInstanceOf(RuntimeException.class)
            .hasCause(cause);

        verifyNoInteractions(messageRecoverer);
    }

    @Test
    @DisplayName("Should log and ignore without recovery or rethrowing when exception matches excluded (false) list")
    void shouldIgnoreAndNotThrowWhenExceptionIsExcluded() {
        // given
        recoverer.withRecoverableExceptions(Map.of(NullPointerException.class, false));

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        org.springframework.messaging.Message<?> springMessage = createSpringMessageWithAmqpSource(amqpMessage);
        Exception cause = new NullPointerException("Ignored error");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // when & Assert
        assertThatCode(() -> recoverer.accept(errorMessage)).doesNotThrowAnyException();

        verifyNoInteractions(messageRecoverer);
    }

    @Test
    @DisplayName("Should successfully recover when a nested cause matches the included exception list")
    void shouldRecoverWithNestedCause() {
        // given
        recoverer.withRecoverableExceptions(Map.of(IllegalArgumentException.class, true));

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        org.springframework.messaging.Message<?> springMessage = createSpringMessageWithAmqpSource(amqpMessage);

        // Target exception is wrapped deep inside
        Exception rootCause = new IllegalArgumentException("Deep cause");
        Exception intermediate = new RuntimeException("Middle layer", rootCause);
        Exception topException = new RuntimeException("Top layer", intermediate);

        ErrorMessage errorMessage = new ErrorMessage(topException, springMessage);

        // when
        recoverer.accept(errorMessage);

        // then
        // NestedExceptionUtils.getMostSpecificCause extracts the deepest root cause
        verify(messageRecoverer).recover(amqpMessage, rootCause);
    }

    @Test
    @DisplayName("Should not call recover if original message does not contain AMQP source data")
    void shouldNotRecoverWhenAmqpMessageIsMissing() {
        // given
        org.springframework.messaging.Message<?> springMessage = new GenericMessage<>("payload without AMQP headers");
        Exception cause = new RuntimeException("Error");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // when
        recoverer.accept(errorMessage);

        // then
        verifyNoInteractions(messageRecoverer);
    }

    @Test
    @DisplayName("Should recover when an explicit Spring NestedRuntimeException subclass is matched directly")
    void shouldRecoverWhenNestedRuntimeExceptionSubclassMatchesDirectly() {
        // Arrange
        recoverer.withRecoverableExceptions(
            Map.of(
                DataIntegrityViolationException.class,
                true // Subclass of NestedRuntimeException
            )
        );

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        var springMessage = createSpringMessageWithAmqpSource(amqpMessage);

        // Simulating a Spring framework DB exception
        Exception cause = new DataIntegrityViolationException("Duplicate key error");
        ErrorMessage errorMessage = new ErrorMessage(cause, springMessage);

        // Act
        recoverer.accept(errorMessage);

        // Assert
        verify(messageRecoverer).recover(amqpMessage, cause);
    }

    @Test
    @DisplayName("Should recover when NestedRuntimeException contains a specific deep cause via .contains()")
    void shouldRecoverWhenNestedRuntimeExceptionContainsTheConfiguredCause() {
        // Arrange
        recoverer.withRecoverableExceptions(Map.of(ConnectException.class, true));

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        var springMessage = createSpringMessageWithAmqpSource(amqpMessage);

        // ConnectException wrapped in Spring's AmqpConnectException (which extends NestedRuntimeException)
        ConnectException structuralRootCause = new ConnectException("Connection refused");
        Exception amqpException = new AmqpConnectException(structuralRootCause);
        ErrorMessage errorMessage = new ErrorMessage(amqpException, springMessage);

        // Act
        recoverer.accept(errorMessage);

        // Assert
        // Note: NestedExceptionUtils.getMostSpecificCause(amqpException) returns structuralRootCause
        verify(messageRecoverer).recover(amqpMessage, structuralRootCause);
    }

    @Test
    @DisplayName("Should recover using a MessageHandlingException")
    void shouldRecoverUsingCustomNestedRuntimeExceptionWrapper() {
        // Arrange
        recoverer.withRecoverableExceptions(Map.of(IllegalArgumentException.class, true));

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        var springMessage = createSpringMessageWithAmqpSource(amqpMessage);

        IllegalArgumentException targetError = new IllegalArgumentException("Inner validation failure");
        Exception messagingException = new MessageHandlingException(springMessage, "Wrapper exception", targetError);
        ErrorMessage errorMessage = new ErrorMessage(messagingException, springMessage);

        // Act
        recoverer.accept(errorMessage);

        // Assert
        verify(messageRecoverer).recover(amqpMessage, targetError);
    }

    @ParameterizedTest(name = "[{index}] Should fail recovery and rethrow when {0}")
    @MethodSource("provideNonRecoverableScenarios")
    @DisplayName("Parameterized verification of non-recoverable exception scenarios")
    void shouldThrowRuntimeExceptionWhenExceptionIsNotRecoverable(
        String testName,
        Map<Class<? extends Throwable>, Boolean> configuration,
        Throwable thrownException,
        Class<? extends Throwable> expectedCauseType
    ) {
        // Arrange
        recoverer.withRecoverableExceptions(configuration);

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        var springMessage = new GenericMessage<>("payload", Map.of("sourceData", amqpMessage));
        ErrorMessage errorMessage = new ErrorMessage(thrownException, springMessage);

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> recoverer.accept(errorMessage))
            .isInstanceOf(MessageRejectedException.class)
            .hasCauseInstanceOf(expectedCauseType);

        // Verify that the recoverer bypassed sending the message downstream
        org.mockito.Mockito.verifyNoInteractions(messageRecoverer);
    }

    @ParameterizedTest(name = "[{index}] Should ignore and exit cleanly when {0}")
    @MethodSource("provideIgnoredScenarios")
    @DisplayName("Parameterized verification of explicitly ignored/excluded exception scenarios")
    void shouldIgnoreAndExitWhenExceptionIsMarkedFalse(
        String testName,
        Map<Class<? extends Throwable>, Boolean> configuration,
        Throwable thrownException
    ) {
        // Arrange
        recoverer.withRecoverableExceptions(configuration);

        Message amqpMessage = new Message(new byte[0], new MessageProperties());
        var springMessage = new GenericMessage<>("payload", Map.of("sourceData", amqpMessage));
        ErrorMessage errorMessage = new ErrorMessage(thrownException, springMessage);

        // Act & Assert
        // AssertJ ensures that the execution path remains completely non-exceptional
        org.assertj.core.api.Assertions.assertThatCode(() -> recoverer.accept(errorMessage)).doesNotThrowAnyException();

        // Verify that the recoverer does not forward the message downstream to recovery
        org.mockito.Mockito.verifyNoInteractions(messageRecoverer);
    }

    /**
     * Supplies test data for scenarios explicitly mapped to false (ignored paths).
     * Arguments format: (Test Display Name, Configuration Map, Actual Thrown Exception)
     */
    private static Stream<Arguments> provideIgnoredScenarios() {
        NullPointerException rootNpe = new NullPointerException("Explicitly ignored pointer issue");

        return Stream.of(
            // Scenario 1: Exact type match on an excluded exception rule
            arguments(
                "exception type directly matches an excluded rule (false)",
                Map.of(OptimisticLockingFailureException.class, false),
                new OptimisticLockingFailureException("Stale object state - ignore logging context")
            ),

            // Scenario 2: Nested lookup matches an excluded exception rule via .contains()
            arguments(
                "nested cause inside NestedRuntimeException matches an excluded rule",
                Map.of(NullPointerException.class, false),
                new AmqpIllegalStateException("Invalid state context wrapper", rootNpe)
            ),

            // Scenario 3: Mixed configuration rules where the exclusion rule matches
            arguments(
                "thrown exception hits the false-configured track in a multi-rule map",
                Map.of(DataIntegrityViolationException.class, true, TransientDataAccessResourceException.class, false),
                new TransientDataAccessResourceException("Network temporary drop - drop from flow")
            )
        );
    }

    /**
     * Supplies test data for explicitly non-recoverable/unconfigured exception paths.
     * Arguments format: (Test Display Name, Configuration Map, Actual Thrown Exception, Expected Type inside the Rethrown Wrapper)
     */
    private static Stream<Arguments> provideNonRecoverableScenarios() {
        return Stream.of(
            // Scenario 1: Not configured at all in the list
            arguments(
                "thrown exception has no configuration rules mapped",
                Map.of(OptimisticLockingFailureException.class, true),
                new DataIntegrityViolationException("Database constraint dropped"),
                DataIntegrityViolationException.class
            ),

            // Scenario 2: Nested structures that do not match the configured types
            arguments(
                "nested cause inside NestedRuntimeException fails configuration checks",
                Map.of(ConnectException.class, true),
                new AmqpIllegalStateException("Invalid AMQP state", new NullPointerException("Root failure context")),
                AmqpIllegalStateException.class
            ),

            // Scenario 3: Explicitly set to 'true' (Included) but evaluation fails on completely different target
            arguments(
                "TransientDataAccessResourceException fails when only OptimisticLocking is allowed",
                Map.of(OptimisticLockingFailureException.class, true),
                new TransientDataAccessResourceException("Deadlock encountered"),
                TransientDataAccessResourceException.class
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionChainsAndTargets")
    @DisplayName("Verify findCause across various types, depths, and structures")
    void shouldFindCauseAccurately(
        String testName,
        Throwable payload,
        Class<? extends Throwable> targetType,
        Optional<? extends Throwable> expectedResult
    ) {
        // Arrange
        // Act
        Optional<? extends Throwable> result = recoverer.findCause(payload, targetType);

        // Assert
        if (expectedResult.isPresent()) {
            assertThat(result).isPresent().get().isEqualTo(expectedResult.get());
        } else {
            assertThat(result).isEmpty();
        }
    }

    private static Stream<Arguments> provideExceptionChainsAndTargets() {
        // Prepare reusable exceptions for precise reference matching
        IllegalArgumentException directTarget = new IllegalArgumentException("Direct error");

        IOException rootIo = new IOException("Disk Full");
        SQLException middleSql = new SQLException("DB connection failure", rootIo);
        RuntimeException topRuntime = new RuntimeException("App crash", middleSql);

        CustomCircularException loop1 = new CustomCircularException("Loop Element A");
        CustomCircularException loop2 = new CustomCircularException("Loop Element B");
        loop1.setCustomCause(loop2);
        loop2.setCustomCause(loop1);

        return Stream.of(
            // 1. Direct match scenarios
            arguments(
                "Direct payload match on target class",
                directTarget,
                IllegalArgumentException.class,
                Optional.of(directTarget)
            ),

            // 2. Nested configuration matching (Various Depths)
            arguments(
                "Nested 1 level deep (Matches middle layer SQLException)",
                topRuntime,
                SQLException.class,
                Optional.of(middleSql)
            ),
            arguments(
                "Nested 2 levels deep (Matches root cause IOException)",
                topRuntime,
                IOException.class,
                Optional.of(rootIo)
            ),

            // 3. No match scenario
            arguments(
                "Returns empty Optional when target type is completely absent from the chain",
                topRuntime,
                NullPointerException.class,
                Optional.empty()
            ),

            // 4. Boundary edge case (Null payload)
            arguments(
                "Returns empty Optional when payload itself is null",
                null,
                RuntimeException.class,
                Optional.empty()
            ),

            // 5. Cyclic graph boundaries
            arguments(
                "Safely handles self-referencing or circular loop chains without blowing up",
                loop1,
                NullPointerException.class,
                Optional.empty()
            )
        );
    }

    /**
     * Custom exception utility allowing explicit mutations to form cyclic dependencies.
     */
    private static class CustomCircularException extends RuntimeException {

        private Throwable structuralCause;

        public CustomCircularException(String message) {
            super(message);
        }

        public void setCustomCause(Throwable cause) {
            this.structuralCause = cause;
        }

        @Override
        public synchronized Throwable getCause() {
            return this.structuralCause;
        }
    }

    // Helper method to simulate a Spring Message mapping an AMQP source message in its headers
    private org.springframework.messaging.Message<?> createSpringMessageWithAmqpSource(Message amqpMessage) {
        return new GenericMessage<>(
            "payload",
            Map.of("sourceData", amqpMessage) // Matches StaticMessageHeaderAccessor.getSourceData internal lookups
        );
    }
}
