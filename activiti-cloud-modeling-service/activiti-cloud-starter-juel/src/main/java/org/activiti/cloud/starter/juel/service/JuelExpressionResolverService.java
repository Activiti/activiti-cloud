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

import java.util.Map;
import org.activiti.cloud.starter.juel.exception.JuelRuntimeException;
import org.activiti.core.el.JuelExpressionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JuelExpressionResolverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JuelExpressionResolverService.class);

    private static final String RESULT = "result";
    private static final String EXPRESSION = "expression";
    private static final String VARIABLES = "variables";

    /**
     * Resolves the given expression with the provided variables.
     * @param inputVariables the input variables: expression and variables.
     * @return the resolved expression.
     */
    public Map<String, Object> resolveExpression(final Map<String, Object> inputVariables) {
        LOGGER.debug("Calling Juel Expression Resolver with parameters {}", inputVariables);
        try {
            final String expression = (String) inputVariables.get(EXPRESSION);
            final Map<String, Object> conditionVariables = (Map<String, Object>) inputVariables.get(VARIABLES);
            return Map.of(
                RESULT,
                new JuelExpressionResolver().resolveExpression(expression, conditionVariables, Object.class)
            );
        } catch (Exception e) {
            throw new JuelRuntimeException(e.getMessage(), e);
        }
    }
}
