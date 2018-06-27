package org.activiti.cloud.services.audit.jpa.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationProcessDefSecuritySpecification implements Specification<AuditEventEntity> {

    private String serviceName;
    private Set<String> processDefinitions = new HashSet<>();

    public ApplicationProcessDefSecuritySpecification(String serviceName,
                                                      Set<String> processDefinitions) {
        this.serviceName = serviceName;
        this.processDefinitions = processDefinitions;
    }

    public Set<String> getProcessDefinitions() {
        return processDefinitions;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Predicate toPredicate(Root<AuditEventEntity> root,
                                 CriteriaQuery<?> criteriaQuery,
                                 CriteriaBuilder criteriaBuilder) {

        List<Predicate> predicates = new ArrayList<>();
        for (String processDef : processDefinitions) {
            Predicate processDefinitionMatchPredicate = criteriaBuilder.equal(root.get("processDefinitionId"),
                                                                              processDef);
            Predicate appNameMatchPredicate = criteriaBuilder.equal(root.get("serviceFullName"),
                                                                    serviceName);

            Predicate appRestrictionPredicate = criteriaBuilder.and(processDefinitionMatchPredicate,
                                                                    appNameMatchPredicate);
            predicates.add(appRestrictionPredicate);
        }

        return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
    }
}
