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
package org.activiti.cloud.services.rest.conf;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VariableValueSizeValidatorTest {

    private static final long MAX_NON_WHITELISTED_BYTES = 8000;

    // --- Map validation (SetProcessVariablesPayload / StartProcessPayload) ---

    @Test
    void should_allowVariables_when_nonWhitelistedTotalWithinLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        Map<String, Object> variables = Map.of("var1", "small value", "var2", "another small value");

        assertThatNoException().isThrownBy(() -> validator.validate(variables));
    }

    @Test
    void should_rejectVariables_when_nonWhitelistedTotalExceedsLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        String largeValue = "x".repeat(5000);
        Map<String, Object> variables = Map.of("var1", largeValue, "var2", largeValue);

        assertThatThrownBy(() -> validator.validate(variables))
            .isInstanceOf(VariableValueTooLargeException.class)
            .hasMessageContaining("exceeds the maximum allowed size of 8000 bytes");
    }

    @Test
    void should_allowLargeWhitelistedVariable_with_smallNonWhitelisted() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Set.of("batchState")
        );

        String hugeValue = "x".repeat(100_000);
        Map<String, Object> variables = new HashMap<>();
        variables.put("batchState", hugeValue);
        variables.put("var1", "small");

        assertThatNoException().isThrownBy(() -> validator.validate(variables));
    }

    @Test
    void should_rejectVariables_when_onlyNonWhitelistedExceedLimit_withWhitelistedPresent() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Set.of("batchState")
        );

        String hugeValue = "x".repeat(100_000);
        String largeNonWhitelisted = "y".repeat(5000);
        Map<String, Object> variables = new HashMap<>();
        variables.put("batchState", hugeValue);
        variables.put("var1", largeNonWhitelisted);
        variables.put("var2", largeNonWhitelisted);

        assertThatThrownBy(() -> validator.validate(variables)).isInstanceOf(VariableValueTooLargeException.class);
    }

    @Test
    void should_allowVariables_when_exactlyAtLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        String exactValue = "x".repeat((int) MAX_NON_WHITELISTED_BYTES);
        Map<String, Object> variables = Map.of("var1", exactValue);

        assertThatNoException().isThrownBy(() -> validator.validate(variables));
    }

    @Test
    void should_allowEmptyVariables() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        assertThatNoException().isThrownBy(() -> validator.validate(Collections.emptyMap()));
    }

    @Test
    void should_allowNullVariables() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        assertThatNoException().isThrownBy(() -> validator.validate(null));
    }

    @Test
    void should_handleNullVariableValue() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", null);

        assertThatNoException().isThrownBy(() -> validator.validate(variables));
    }

    // --- Single variable validation (CreateTaskVariablePayload / UpdateTaskVariablePayload) ---

    @Test
    void should_allowSingleVariable_when_withinLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        assertThatNoException().isThrownBy(() -> validator.validateSingle("var1", "small value"));
    }

    @Test
    void should_rejectSingleVariable_when_exceedsLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Collections.emptySet()
        );

        String largeValue = "x".repeat((int) MAX_NON_WHITELISTED_BYTES + 1);

        assertThatThrownBy(() -> validator.validateSingle("var1", largeValue))
            .isInstanceOf(VariableValueTooLargeException.class);
    }

    @Test
    void should_allowSingleWhitelistedVariable_when_exceedsLimit() {
        VariableValueSizeValidator validator = new VariableValueSizeValidator(
            MAX_NON_WHITELISTED_BYTES,
            Set.of("batchState")
        );

        String largeValue = "x".repeat(100_000);

        assertThatNoException().isThrownBy(() -> validator.validateSingle("batchState", largeValue));
    }

    // --- VariableValueTooLargeException ---

    @Test
    void should_containSizesInExceptionMessage() {
        VariableValueTooLargeException ex = new VariableValueTooLargeException(10000, 8000);
        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).isEqualTo(
            "Non-whitelisted variable data of 10000 bytes exceeds the maximum allowed size of 8000 bytes"
        );
    }
}
