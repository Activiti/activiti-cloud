package org.activiti.cloud.services.audit.jpa.security;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSecuritySpecification implements Specification<AuditEventEntity> {

    private String serviceName;

    public ApplicationSecuritySpecification(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Predicate toPredicate(Root<AuditEventEntity> root,
                                 CriteriaQuery<?> criteriaQuery,
                                 CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.equal(root.get("serviceFullName"),
                                     serviceName);
    }
}
