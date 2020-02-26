package org.activiti.cloud.services.audit.jpa.security;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class ImpossibleSpecification implements Specification<AuditEventEntity> {

    public ImpossibleSpecification() {

    }

    @Override
    public Predicate toPredicate(Root<AuditEventEntity> root,
                                 CriteriaQuery<?> criteriaQuery,
                                 CriteriaBuilder criteriaBuilder) {
        Predicate impossibleProcessInstance = criteriaBuilder.equal(root.get("processInstanceId"),
                                                                    "0");
        Predicate impossibleProcessDefinition = criteriaBuilder.equal(root.get("processDefinitionId"),
                                                                      "0");

        return criteriaBuilder.and(impossibleProcessInstance,
                                   impossibleProcessDefinition);
    }
}
