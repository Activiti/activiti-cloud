package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.temporal.Temporal;
import java.util.Map;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.AbstractVariableEntity_;

public class VariableSelection<R, K extends AbstractVariableEntity, V extends Comparable<V>> {

    private final From<R, K> root;
    private final Predicate selectionPredicate;
    private Expression<V> selectionExpression;
    protected final Class<V> variableJavaType;
    protected final CriteriaBuilder criteriaBuilder;

    public VariableSelection(
        From<R, K> root,
        Map<Path<String>, String> selectionFilters,
        Class<V> variableJavaType,
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

    public Expression<V> getExtractedValue() {
        Expression extracted;
        if (Number.class.isAssignableFrom(variableJavaType)) {
            extracted =
                criteriaBuilder.function(
                    CustomPostgreSQLDialect.EXTRACT_JSON_NUMERIC_VALUE,
                    variableJavaType,
                    root.get(AbstractVariableEntity_.value)
                );
        } else if (variableJavaType == Boolean.class) {
            Expression<Boolean> function = criteriaBuilder.function(
                CustomPostgreSQLDialect.EXTRACT_JSON_BOOLEAN_VALUE,
                Boolean.class,
                root.get(AbstractVariableEntity_.value)
            );
            extracted = criteriaBuilder.selectCase().when(function, 1).otherwise(0);
        } else {
            extracted =
                criteriaBuilder.function(
                    CustomPostgreSQLDialect.EXTRACT_JSON_STRING_VALUE,
                    variableJavaType,
                    root.get(AbstractVariableEntity_.value)
                );
        }
        if (Temporal.class.isAssignableFrom(variableJavaType)) {
            extracted = extracted.as(variableJavaType);
        }
        return extracted;
    }

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
