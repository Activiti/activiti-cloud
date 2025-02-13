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
import java.util.Map;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.AbstractVariableEntity_;

public class VariableSelectionExpressionImpl<R, K extends AbstractVariableEntity>
    implements VariableSelectionExpression {

    protected final From<R, K> root;
    private final Predicate selectionPredicate;
    private Expression<?> selectionExpression;
    protected final Class<?> variableJavaType;
    protected final CriteriaBuilder criteriaBuilder;

    public VariableSelectionExpressionImpl(
        From<R, K> root,
        Map<Path<String>, String> selectionFilters,
        Class<?> variableJavaType,
        CriteriaBuilder criteriaBuilder
    ) {
        this.root = root;
        this.variableJavaType = variableJavaType;
        this.criteriaBuilder = criteriaBuilder;
        this.selectionPredicate =
            criteriaBuilder.and(
                selectionFilters
                    .entrySet()
                    .stream()
                    .map(entry -> criteriaBuilder.equal(entry.getKey(), entry.getValue()))
                    .toArray(Predicate[]::new)
            );
    }

    public Expression getExtractedValue() {
        String extractionFunctionName = CustomPostgreSQLDialect.getExtractionFunctionName(variableJavaType);
        Class<?> extractionFunctionReturnType = variableJavaType == Boolean.class ||
            variableJavaType == BigDecimal.class
            ? variableJavaType
            : String.class;
        Expression<?> extractedValue = criteriaBuilder.function(
            extractionFunctionName,
            extractionFunctionReturnType,
            root.get(AbstractVariableEntity_.value)
        );
        if (variableJavaType != BigDecimal.class) {
            Class<?> castType = variableJavaType == Boolean.class ? Integer.class : variableJavaType;
            extractedValue = extractedValue.as(castType);
        }
        return extractedValue;
    }

    @Override
    public Expression getSelectionExpression() {
        if (selectionExpression == null) {
            selectionExpression =
                criteriaBuilder.greatest(
                    (Expression) criteriaBuilder
                        .selectCase()
                        .when(selectionPredicate, getExtractedValue())
                        .otherwise(criteriaBuilder.nullLiteral(variableJavaType))
                );
        }
        return selectionExpression;
    }
}
