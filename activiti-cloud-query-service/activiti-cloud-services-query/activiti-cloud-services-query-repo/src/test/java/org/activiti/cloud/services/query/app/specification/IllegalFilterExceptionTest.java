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
package org.activiti.cloud.services.query.app.specification;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.query.app.filter.FilterOperator;
import org.activiti.cloud.services.query.app.filter.VariableType;
import org.junit.jupiter.api.Test;

class IllegalFilterExceptionTest {

    @Test
    void should_preserveTheOriginalCause_forDiagnosability() {
        NumberFormatException cause = new NumberFormatException("not a number");

        IllegalFilterException ex = new IllegalFilterException(
            VariableType.BIGDECIMAL,
            FilterOperator.GREATER_THAN,
            "not-a-number",
            cause
        );

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo(
            "Illegal filter for variable type BIGDECIMAL. Operator: GREATER_THAN, value: not-a-number"
        );
    }
}
