package org.activiti.cloud.services.audit.jpa.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
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
            //don't actually have definitionKey in the event but do have definitionId which should contain it
            // format is e.g. SimpleProcess:version:id
            // and fact we're here means can't be wildcard
            Predicate processDefinitionMatchPredicate = criteriaBuilder.like(root.get("processDefinitionId"),
                                                                              processDef+"%");

            Expression<String> replacedServiceName = root.get("serviceName");

            replacedServiceName = criteriaBuilder.function("REPLACE",String.class,replacedServiceName,
                    criteriaBuilder.literal("-"),criteriaBuilder.literal(""));

            Predicate appNameMatchPredicate = criteriaBuilder.equal(
                    criteriaBuilder.upper(replacedServiceName),
                    serviceName.replace("-","").toUpperCase());

            Predicate appRestrictionPredicate = criteriaBuilder.and(processDefinitionMatchPredicate,
                    appNameMatchPredicate);
            predicates.add(appRestrictionPredicate);
        }

        return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
    }
}
