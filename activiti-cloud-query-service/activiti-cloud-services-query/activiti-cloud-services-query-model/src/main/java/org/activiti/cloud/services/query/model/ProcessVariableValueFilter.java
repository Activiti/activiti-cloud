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

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;

public class ProcessVariableValueFilter {

    private final String variableKey;
    private final Object filteredValue;
    private final ProcessVariableFilterType filterType;

    public ProcessVariableValueFilter(String variableKey, Object filteredValue, ProcessVariableFilterType filterType) {
        this.variableKey = variableKey;
        this.filteredValue = filteredValue;
        this.filterType = filterType;
    }

    public BooleanExpression getExpression() {
        QProcessVariableEntity processVariableEntity = QProcessVariableEntity.processVariableEntity;
        StringTemplate extractedValue = Expressions.stringTemplate(
            "cast(jsonb_extract_path({0}, 'value') as string)",
            processVariableEntity.value
        );

        BooleanExpression valueExpression =
            switch (filterType) {
                case CONTAINS -> extractedValue.containsIgnoreCase(Expressions.constant(filteredValue.toString()));
                default -> extractedValue.equalsIgnoreCase(Expressions.constant(filteredValue.toString()));
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

    public Object getFilteredValue() {
        return filteredValue;
    }
}
