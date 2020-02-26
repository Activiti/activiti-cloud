package org.activiti.cloud.services.audit.jpa.repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class EventSpecification implements Specification<AuditEventEntity> {

    private SpecSearchCriteria criteria;

    public EventSpecification(final SpecSearchCriteria criteria) {
        super();
        this.criteria = criteria;
    }

    public SpecSearchCriteria getCriteria() {
        return criteria;
    }

    @Override
    public Predicate toPredicate(final Root<AuditEventEntity> root,
                                 final CriteriaQuery<?> query,
                                 final CriteriaBuilder builder) {
        switch (criteria.getOperation()) {
            case EQUALITY:
                return builder.equal(root.get(criteria.getKey()),
                                     criteria.getValue());
            case NEGATION:
                return builder.notEqual(root.get(criteria.getKey()),
                                        criteria.getValue());
            case GREATER_THAN:
                return builder.greaterThan(root.get(criteria.getKey()),
                                           criteria.getValue().toString());
            case LESS_THAN:
                return builder.lessThan(root.get(criteria.getKey()),
                                        criteria.getValue().toString());
            case LIKE:
                return builder.like(root.get(criteria.getKey()),
                                    criteria.getValue().toString());
            case STARTS_WITH:
                return builder.like(root.get(criteria.getKey()),
                                    criteria.getValue() + "%");
            case ENDS_WITH:
                return builder.like(root.get(criteria.getKey()),
                                    "%" + criteria.getValue());
            case CONTAINS:
                return builder.like(root.get(criteria.getKey()),
                                    "%" + criteria.getValue() + "%");
            default:
                return null;
        }
    }
}
