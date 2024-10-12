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
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableType;

public class BooleanVariableValueCondition extends VariableValueCondition {

    public BooleanVariableValueCondition(
        Path<?> path,
        FilterOperator operator,
        String value,
        CriteriaBuilder criteriaBuilder
    ) {
        super(path, operator, value, criteriaBuilder);
    }

    @Override
    protected String getFunctionName() {
        if (operator == FilterOperator.EQUALS) {
            return CustomPostgreSQLDialect.JSON_VALUE_EQUALS;
        }
        throw new IllegalFilterException(VariableType.BOOLEAN, operator);
    }

    @Override
    protected Boolean getConvertedValue() {
        return Boolean.parseBoolean(value);
    }
}
