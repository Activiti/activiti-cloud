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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.activiti.cloud.services.query.model.dialect.JsonValueFunctions;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableType;

public class IntegerVariableValueCondition extends VariableValueCondition {

    public IntegerVariableValueCondition(
        Path<?> path,
        FilterOperator operator,
        String value,
        CriteriaBuilder criteriaBuilder
    ) {
        super(path, operator, value, criteriaBuilder);
    }

    @Override
    protected String getFunctionName() {
        return switch (operator) {
            case EQUALS -> JsonValueFunctions.NUMERIC_EQUALS;
            case GREATER_THAN -> JsonValueFunctions.NUMERIC_GREATER_THAN;
            case GREATER_THAN_OR_EQUAL -> JsonValueFunctions.NUMERIC_GREATER_THAN_EQUAL;
            case LESS_THAN -> JsonValueFunctions.NUMERIC_LESS_THAN;
            case LESS_THAN_OR_EQUAL -> JsonValueFunctions.NUMERIC_LESS_THAN_EQUAL;
            default -> throw new IllegalFilterException(VariableType.INTEGER, operator);
        };
    }

    @Override
    protected Integer getConvertedValue() {
        return Integer.parseInt(value);
    }
}
