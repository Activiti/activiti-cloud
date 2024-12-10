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
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public class VariableValueCondition<R, K, V> extends VariableSelection {

    private final VariableFilter filter;

    protected VariableValueCondition(
        From<R, K> root,
        Map<Path<String>, String> selectionFilters,
        Class<V> variableJavaType,
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder
    ) {
        super(root, selectionFilters, variableJavaType, criteriaBuilder);
        this.filter = filter;
    }

    public VariableFilter getFilter() {
        return filter;
    }

    public Predicate toPredicate() {
        return switch (filter.operator()) {
            case EQUALS -> criteriaBuilder.equal(getSelectionExpression(), getConvertedValue());
            case NOT_EQUALS -> criteriaBuilder.notEqual(getSelectionExpression(), getConvertedValue());
            case GREATER_THAN -> criteriaBuilder.greaterThan(getSelectionExpression(), getConvertedValue());
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(
                getSelectionExpression(),
                getConvertedValue()
            );
            case LESS_THAN -> criteriaBuilder.lessThan(getSelectionExpression(), getConvertedValue());
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(getSelectionExpression(), getConvertedValue());
            case LIKE -> criteriaBuilder.like(
                criteriaBuilder.lower((Expression<String>) getSelectionExpression()),
                "%" + filter.value().toLowerCase() + "%"
            );
        };
    }

    protected Expression getConvertedValue() {
        if (variableJavaType == String.class) {
            return criteriaBuilder.literal(filter.value());
        } else if (variableJavaType == Integer.class) {
            return criteriaBuilder.literal(Integer.parseInt(filter.value()));
        } else if (variableJavaType == BigDecimal.class) {
            return criteriaBuilder.literal(new BigDecimal(filter.value()));
        } else if (variableJavaType == Boolean.class) {
            return criteriaBuilder.literal(Boolean.parseBoolean(filter.value()) ? 1 : 0);
        } else if (variableJavaType == LocalDate.class) {
            return criteriaBuilder.literal(filter.value()).as(LocalDate.class);
        } else if (variableJavaType == LocalDateTime.class) {
            return criteriaBuilder.literal(filter.value()).as(LocalDateTime.class);
        } else {
            throw new IllegalArgumentException("Unsupported variable type: " + variableJavaType);
        }
    }
}
