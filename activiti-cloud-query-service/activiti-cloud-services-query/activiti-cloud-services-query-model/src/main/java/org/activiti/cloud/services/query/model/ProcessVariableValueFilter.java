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
package org.activiti.cloud.services.query.model;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

public class ProcessVariableValueFilter {

    private final String variableKey;
    private final Object exactValue;
    private final ProcessVariableFilterType filterType;

    public ProcessVariableValueFilter(String variableKey, Object filteredValue, ProcessVariableFilterType filterType) {
        this.variableKey = variableKey;
        this.exactValue = filteredValue;
        this.filterType = filterType;
    }

    public BooleanExpression getExpression() {
        QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;

        BooleanExpression valueExpression =
            switch (filterType) {
                case CONTAINS -> Expressions
                    .stringTemplate(
                        "cast(jsonb_extract_path_text({0}, 'value') as string)",
                        processVariableEntity.value
                    )
                    .containsIgnoreCase(Expressions.constant(String.valueOf(exactValue)));
                case EQUALS -> {
                    if (exactValue instanceof Integer) {
                        yield Expressions
                            .stringTemplate(
                                "CAST(jsonb_extract_path({0}, 'value') as INTEGER)",
                                processVariableEntity.value
                            )
                            .eq(Expressions.constant(exactValue));
                    }
                    yield Expressions
                        .stringTemplate(
                            "CAST(jsonb_extract_path_text({0}, 'value') as STRING)",
                            processVariableEntity.value
                        )
                        .equalsIgnoreCase(Expressions.constant(String.valueOf(exactValue)));
                }
                default -> Expressions
                    .stringTemplate(
                        "CAST(jsonb_extract_path_text({0}, 'value') as STRING)",
                        processVariableEntity.value
                    )
                    .equalsIgnoreCase(Expressions.constant(String.valueOf(exactValue)));
            };
        return processVariableEntity.processDefinitionKey
            .concat("/")
            .concat(processVariableEntity.name)
            .eq(variableKey)
            .and(valueExpression);
    }

    public String getVariableKey() {
        return variableKey;
    }

    public Object getExactValue() {
        return exactValue;
    }
}
