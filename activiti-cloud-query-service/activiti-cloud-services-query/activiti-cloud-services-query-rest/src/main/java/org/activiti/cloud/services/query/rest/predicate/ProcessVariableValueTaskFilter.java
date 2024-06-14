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

package org.activiti.cloud.services.query.rest.predicate;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.validation.constraints.NotNull;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;

public class ProcessVariableValueTaskFilter implements QueryDslPredicateFilter {

    private final String variableKey;
    private final Object variableValue;
    private final ProcessVariableFilterType filterType;

    public ProcessVariableValueTaskFilter(
        String variableKey,
        Object variableValue,
        ProcessVariableFilterType filterType
    ) {
        this.variableKey = variableKey;
        this.variableValue = variableValue;
        this.filterType = filterType;
    }

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;

        BooleanExpression condition = processVariableEntity.processDefinitionKey
            .concat("/")
            .concat(processVariableEntity.name)
            .eq(variableKey)
            .and(
                Expressions
                    .stringTemplate("json_extract_path_text({0}, 'value')", processVariableEntity.value)
                    .eq(Expressions.constant(variableValue))
            );
        return condition.or(currentPredicate);
    }

    public enum ProcessVariableFilterType {
        EQUALS,
        CONTAINS,
        RANGE,
    }
}
