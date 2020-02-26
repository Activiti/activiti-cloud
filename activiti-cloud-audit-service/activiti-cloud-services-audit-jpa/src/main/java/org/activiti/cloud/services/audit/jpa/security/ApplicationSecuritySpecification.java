package org.activiti.cloud.services.audit.jpa.security;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
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

        Expression<String> replacedServiceName = root.get("serviceName");

        replacedServiceName = criteriaBuilder.function("REPLACE",String.class,replacedServiceName,
                criteriaBuilder.literal("-"),criteriaBuilder.literal(""));

        return criteriaBuilder.equal(
                criteriaBuilder.upper(replacedServiceName),
                                     serviceName.replace("-","").toUpperCase());
    }
}
