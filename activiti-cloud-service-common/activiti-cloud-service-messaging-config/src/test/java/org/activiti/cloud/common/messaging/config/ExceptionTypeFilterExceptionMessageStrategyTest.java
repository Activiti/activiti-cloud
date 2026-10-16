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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceptionTypeFilterExceptionMessageStrategyTest {

    private ExceptionTypeFilterExceptionMessageStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ExceptionTypeFilterExceptionMessageStrategy();
    }

    @Test
    @DisplayName("Should return true by default when no exceptions are configured")
    void shouldReturnFalseByDefault() {
        // Arrange
        Throwable throwable = new IllegalArgumentException("Test exception");

        // Act
        boolean isFatal = strategy.isUserCauseFatal(throwable);

        // Assert
        assertThat(isFatal).as("By default, unconfigured exceptions should not be fatal").isFalse();
    }

    @Test
    @DisplayName("Should return true when exception matches an included (fatal) type")
    void shouldReturnTrueWhenExceptionIsFatal() {
        // Arrange
        Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
        exceptionsMap.put(IllegalArgumentException.class, true); // true means included/fatal

        strategy.withUserFatalExceptions(exceptionsMap);
        Throwable fatalException = new IllegalArgumentException("Fatal error");

        // Act & Assert
        assertThat(strategy.isUserCauseFatal(fatalException))
            .as("An explicitly included exception type should be marked fatal")
            .isTrue();
    }

    @Test
    @DisplayName("Should return false when exception matches an excluded (recoverable) type")
    void shouldReturnFalseWhenExceptionIsRecoverable() {
        // Arrange
        Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
        exceptionsMap.put(IOException.class, false); // false means excluded/recoverable

        strategy.withUserFatalExceptions(exceptionsMap);
        Throwable recoverableException = new IOException("Recoverable error");

        // Act & Assert
        assertThat(strategy.isUserCauseFatal(recoverableException))
            .as("An explicitly excluded exception type should not be marked fatal")
            .isFalse();
    }

    @Test
    @DisplayName("Should evaluate inheritance matching based on Spring ExceptionTypeFilter hierarchy rules")
    void shouldEvaluateInheritanceCorrectly() {
        // Arrange
        Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
        exceptionsMap.put(RuntimeException.class, true); // Base runtime exceptions are fatal
        exceptionsMap.put(NullPointerException.class, false); // Specific subclass is recoverable

        strategy.withUserFatalExceptions(exceptionsMap);

        Throwable genericRuntimeException = new IllegalArgumentException("Generic runtime");
        Throwable specificRecoverableException = new NullPointerException("Specific NPE");

        // Act & Assert
        // 1. Subclasses of RuntimeException (like IllegalArgumentException) match the 'true' rule
        assertThat(strategy.isUserCauseFatal(genericRuntimeException))
            .as("Subclasses of an included base class should inherit the fatal marking")
            .isTrue();

        // 2. Exact match on the specific rule overrides the broader base class rule
        assertThat(strategy.isUserCauseFatal(specificRecoverableException))
            .as("A specific rule should override a generic base class rule")
            .isFalse();
    }

    @Test
    @DisplayName("Should allow fluent chaining using the 'with' configuration method")
    void shouldSupportFluentChaining() {
        // Act
        ExceptionTypeFilterExceptionMessageStrategy returnedStrategy = strategy.withUserFatalExceptions(
            Map.of(IllegalArgumentException.class, true)
        );

        // Assert
        assertThat(returnedStrategy)
            .as("The initialization method should return the instance for fluent chaining")
            .isSameAs(strategy);
    }
}
