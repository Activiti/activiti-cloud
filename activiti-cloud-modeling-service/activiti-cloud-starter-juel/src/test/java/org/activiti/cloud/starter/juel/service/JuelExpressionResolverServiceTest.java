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
package org.activiti.cloud.starter.juel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.starter.juel.exception.JuelRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JuelExpressionResolverServiceTest {

    private static final String RESULT = "result";
    private static final String EXPRESSION = "expression";
    private static final String VARIABLES = "variables";
    private static final String RESULT_TEST = "10 + 20";

    @Autowired
    JuelExpressionResolverService juelExpressionResolverService;

    @Test
    public void should_outputIsCorrect_when_resolveExpression() {
        final String expression = "${var1} + ${var2}";
        final Map<String, Object> variables = Map.of("var1", 10, "var2", 20);
        final Map<String, Object> input = Map.of(EXPRESSION, expression, VARIABLES, variables);
        final Map<String, Object> result = juelExpressionResolverService.resolveExpression(input);
        assertThat(result.get(RESULT)).isNotNull();
        assertThat(result.get(RESULT)).isEqualTo(RESULT_TEST);
    }

    @Test
    public void should_throwsJuelRuntimeException_when_resolveInvalidExpression() {
        final String expression = "${var1} + ${var2}";
        final Map<String, Object> variables = new HashMap<>();
        final Map<String, Object> input = Map.of(EXPRESSION, expression, VARIABLES, variables);
        Throwable thrown = catchThrowable(() -> juelExpressionResolverService.resolveExpression(input));
        assertThat(thrown).isInstanceOf(JuelRuntimeException.class);
    }
}
