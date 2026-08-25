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
package org.activiti.cloud.services.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VariableValueSizeValidatorTest {

    private VariableProperties variableProperties;
    private VariableValueSizeValidator validator;

    @BeforeEach
    void setUp() {
        variableProperties = new VariableProperties();
        validator = new VariableValueSizeValidator(new ObjectMapper(), variableProperties);
    }

    @Test
    void shouldDefaultMaxValueSizeTo5Mb() {
        assertThat(variableProperties.getMaxValueSize()).isEqualTo(5 * 1024 * 1024);
    }

    @Test
    void shouldPassWhenValueIsUnderLimit() {
        variableProperties.setMaxValueSize(5);

        assertThatCode(() -> validator.validate("name", "ab")).doesNotThrowAnyException();
    }

    @Test
    void shouldPassWhenValueIsExactlyAtLimit() {
        variableProperties.setMaxValueSize(5);

        assertThatCode(() -> validator.validate("name", "abc")).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenValueIsOverLimit() {
        variableProperties.setMaxValueSize(5);

        assertThatThrownBy(() -> validator.validate("name", "abcd"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Variable 'name' value exceeds maximum allowed size of 5 bytes");
    }

    @Test
    void shouldPassWhenLimitIsZero() {
        variableProperties.setMaxValueSize(0);

        assertThatCode(() -> validator.validate("name", "abcd")).doesNotThrowAnyException();
    }

    @Test
    void shouldPassWhenLimitIsNegative() {
        variableProperties.setMaxValueSize(-1);

        assertThatCode(() -> validator.validate("name", "abcd")).doesNotThrowAnyException();
    }
}
