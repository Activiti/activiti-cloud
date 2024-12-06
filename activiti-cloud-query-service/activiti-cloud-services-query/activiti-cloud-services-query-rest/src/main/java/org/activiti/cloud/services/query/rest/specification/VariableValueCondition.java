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
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public abstract class VariableValueCondition {

    protected final Path<?> variableValuePath;
    protected final Predicate variablePredicate;
    protected final FilterOperator operator;
    protected final String filterValue;
    protected final CriteriaBuilder criteriaBuilder;
    private Expression columnExpression;

    protected VariableValueCondition(
        Path<?> variableValuePath,
        Predicate variablePredicate,
        FilterOperator operator,
        String filterValue,
        CriteriaBuilder criteriaBuilder
    ) {
        this.variableValuePath = variableValuePath;
        this.variablePredicate = variablePredicate;
        this.operator = operator;
        this.filterValue = filterValue;
        this.criteriaBuilder = criteriaBuilder;
    }

    public abstract Expression<?> getExtractedValue();

    protected abstract Expression getConvertedValue();

    public Predicate toPredicate() {
        return switch (operator) {
            case EQUALS -> criteriaBuilder.equal(getColumnExpression(), getConvertedValue());
            case NOT_EQUALS -> criteriaBuilder.notEqual(getColumnExpression(), getConvertedValue());
            case GREATER_THAN -> criteriaBuilder.greaterThan(getColumnExpression(), getConvertedValue());
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(
                getColumnExpression(),
                getConvertedValue()
            );
            case LESS_THAN -> criteriaBuilder.lessThan(getColumnExpression(), getConvertedValue());
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(getColumnExpression(), getConvertedValue());
            case LIKE -> criteriaBuilder.like(
                criteriaBuilder.lower((Expression<String>) getColumnExpression()),
                "%" + filterValue.toLowerCase() + "%"
            );
        };
    }

    public Expression getColumnExpression() {
        if (columnExpression == null) {
            columnExpression =
                criteriaBuilder.greatest(
                    (Expression) criteriaBuilder
                        .selectCase()
                        .when(variablePredicate, getExtractedValue())
                        .otherwise(criteriaBuilder.nullLiteral(getExtractedValue().getJavaType()))
                );
        }
        return columnExpression;
    }
}
