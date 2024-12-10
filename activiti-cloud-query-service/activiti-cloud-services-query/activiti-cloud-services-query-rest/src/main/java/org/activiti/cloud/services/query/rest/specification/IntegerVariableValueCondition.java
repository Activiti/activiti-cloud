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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.model.VariableValue;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public class IntegerVariableValueCondition extends VariableValueCondition {

    public IntegerVariableValueCondition(
        Path<VariableValue> variableValuePath,
        Predicate variablePredicate,
        FilterOperator operator,
        String value,
        CriteriaBuilder criteriaBuilder
    ) {
        super(variableValuePath, variablePredicate, operator, value, criteriaBuilder);
    }

    @Override
    public Expression getExtractedValue() {
        return criteriaBuilder.function(
            CustomPostgreSQLDialect.EXTRACT_JSON_NUMERIC_VALUE,
            Integer.class,
            variableValuePath
        );
    }

    @Override
    protected Expression getConvertedValue() {
        return criteriaBuilder.literal(Integer.parseInt(filterValue));
    }
}
