package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.Predicate;

public interface VariableValueFilterCondition {
    Predicate getPredicate();
}
