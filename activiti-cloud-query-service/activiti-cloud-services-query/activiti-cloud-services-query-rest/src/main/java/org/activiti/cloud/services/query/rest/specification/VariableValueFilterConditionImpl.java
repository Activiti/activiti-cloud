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
import java.util.Map;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class VariableValueFilterConditionImpl<R, K extends AbstractVariableEntity>
    extends VariableSelectionExpressionImpl<R, K>
    implements VariableValueFilterCondition {

    private final VariableFilter filter;

    protected VariableValueFilterConditionImpl(
        From<R, K> root,
        Map<Path<String>, String> selectionFilters,
        Class<?> variableJavaType,
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder
    ) {
        super(root, selectionFilters, variableJavaType, criteriaBuilder);
        this.filter = filter;
    }

    public VariableFilter getFilter() {
        return filter;
    }

    @Override
    public Predicate getPredicate() {
        try {
            return switch (filter.operator()) {
                case EQUALS -> criteriaBuilder.equal(getSelectionExpression(), getConvertedFilterValue());
                case NOT_EQUALS -> criteriaBuilder.notEqual(getSelectionExpression(), getConvertedFilterValue());
                case GREATER_THAN -> criteriaBuilder.greaterThan(getSelectionExpression(), getConvertedFilterValue());
                case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(
                    getSelectionExpression(),
                    getConvertedFilterValue()
                );
                case LESS_THAN -> criteriaBuilder.lessThan(getSelectionExpression(), getConvertedFilterValue());
                case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(
                    getSelectionExpression(),
                    getConvertedFilterValue()
                );
                case LIKE -> criteriaBuilder.like(
                    criteriaBuilder.lower((Expression<String>) getSelectionExpression()),
                    "%" + filter.value().toLowerCase() + "%"
                );
            };
        } catch (FunctionArgumentException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Illegal filter for variable type %s. Operator: %s, value: %s".formatted(
                        filter.type(),
                        filter.operator(),
                        filter.value()
                    )
            );
        }
    }

    private Expression getConvertedFilterValue() {
        if (variableJavaType == Boolean.class) {
            return criteriaBuilder.literal(Boolean.parseBoolean(filter.value()) ? 1 : 0);
        }
        Expression<String> value = criteriaBuilder.literal(filter.value());
        return variableJavaType == String.class ? value : value.as(variableJavaType);
    }
}
