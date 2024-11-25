package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableType;

public abstract class NumericVariableValueCondition extends VariableValueCondition {

    public abstract VariableType getVariableType();

    public NumericVariableValueCondition(
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
            case EQUALS -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_EQUALS;
            case NOT_EQUALS -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_NOT_EQUALS;
            case GREATER_THAN -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_GREATER_THAN;
            case GREATER_THAN_OR_EQUAL -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_GREATER_THAN_EQUAL;
            case LESS_THAN -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_LESS_THAN;
            case LESS_THAN_OR_EQUAL -> CustomPostgreSQLDialect.JSON_VALUE_NUMERIC_LESS_THAN_EQUAL;
            default -> throw new IllegalFilterException(getVariableType(), operator);
        };
    }
}
