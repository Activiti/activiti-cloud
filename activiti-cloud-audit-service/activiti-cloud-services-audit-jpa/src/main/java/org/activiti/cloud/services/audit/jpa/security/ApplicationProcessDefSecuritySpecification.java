/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.audit.jpa.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationProcessDefSecuritySpecification implements Specification<AuditEventEntity> {

    private String serviceName;
    private Set<String> processDefinitions = new HashSet<>();

    public ApplicationProcessDefSecuritySpecification(String serviceName, Set<String> processDefinitions) {
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
    public Predicate toPredicate(
        Root<AuditEventEntity> root,
        CriteriaQuery<?> criteriaQuery,
        CriteriaBuilder criteriaBuilder
    ) {
        List<Predicate> predicates = new ArrayList<>();
        for (String processDef : processDefinitions) {
            //don't actually have definitionKey in the event but do have definitionId which should contain it
            // format is e.g. SimpleProcess:version:id
            // and fact we're here means can't be wildcard
            Predicate processDefinitionMatchPredicate = criteriaBuilder.like(
                root.get("processDefinitionId"),
                processDef + "%"
            );

            Expression<String> replacedServiceName = root.get("serviceName");

            replacedServiceName =
                criteriaBuilder.function(
                    "REPLACE",
                    String.class,
                    replacedServiceName,
                    criteriaBuilder.literal("-"),
                    criteriaBuilder.literal("")
                );

            Predicate appNameMatchPredicate = criteriaBuilder.equal(
                criteriaBuilder.upper(replacedServiceName),
                serviceName.replace("-", "").toUpperCase()
            );

            Predicate appRestrictionPredicate = criteriaBuilder.and(
                processDefinitionMatchPredicate,
                appNameMatchPredicate
            );
            predicates.add(appRestrictionPredicate);
        }

        return criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
    }
}
