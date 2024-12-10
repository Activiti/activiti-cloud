package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.Expression;

public interface VariableSelectionExpression {
    Expression<?> getSelectionExpression();
}
