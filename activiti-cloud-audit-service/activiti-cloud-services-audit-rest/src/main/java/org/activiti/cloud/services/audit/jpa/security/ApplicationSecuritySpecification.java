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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSecuritySpecification implements Specification<AuditEventEntity> {

    private String serviceName;

    public ApplicationSecuritySpecification(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Predicate toPredicate(
        Root<AuditEventEntity> root,
        CriteriaQuery<?> criteriaQuery,
        CriteriaBuilder criteriaBuilder
    ) {
        Expression<String> replacedServiceName = root.get("serviceName");

        replacedServiceName =
            criteriaBuilder.function(
                "REPLACE",
                String.class,
                replacedServiceName,
                criteriaBuilder.literal("-"),
                criteriaBuilder.literal("")
            );

        return criteriaBuilder.equal(
            criteriaBuilder.upper(replacedServiceName),
            serviceName.replace("-", "").toUpperCase()
        );
    }
}
