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

package org.activiti.cloud.services.security;

import com.introproventures.graphql.jpa.query.schema.RestrictedKeysProvider;
import com.introproventures.graphql.jpa.query.schema.impl.EntityIntrospector;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.QProcessDefinitionEntity;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

public class ActivitiRestrictedKeysProvider implements RestrictedKeysProvider {

    private final ProcessDefinitionRestrictionService processDefinitionRestrictionService;

    private final EntityManager entityManager;

    public ActivitiRestrictedKeysProvider(
        EntityManagerFactory entityManagerFactory,
        ProcessDefinitionRestrictionService processDefinitionRestrictionService
    ) {
        this.processDefinitionRestrictionService = processDefinitionRestrictionService;
        this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    @Override
    public Optional<List<Object>> apply(EntityIntrospector.EntityIntrospectionResult entityDescriptor) {
        return Optional
            .of(entityDescriptor.getEntity())
            .filter(ProcessDefinitionEntity.class::equals)
            .map(entityClass -> {
                var predicate = processDefinitionRestrictionService.restrictProcessDefinitionQuery(
                    new BooleanBuilder(),
                    SecurityPolicyAccess.READ
                );

                var entity = QProcessDefinitionEntity.processDefinitionEntity;

                JPAQuery<?> query = new JPAQuery<QProcessDefinitionEntity>(entityManager)
                    .from(entity)
                    .select(entity.id)
                    .where(predicate);

                return query.fetch().stream().map(Object.class::cast).toList();
            })
            .or(() -> Optional.of(List.of()));
    }
}
