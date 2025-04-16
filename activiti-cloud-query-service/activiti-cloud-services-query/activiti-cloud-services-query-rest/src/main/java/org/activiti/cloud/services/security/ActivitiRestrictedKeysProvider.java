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
import java.util.function.Supplier;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

public class ActivitiRestrictedKeysProvider implements RestrictedKeysProvider {

    private final ProcessDefinitionRestrictionService processDefinitionRestrictionService;
    private final ProcessInstanceRestrictionService processInstanceRestrictionService;
    private final ProcessVariableRestrictionService processVariableRestrictionService;
    private final TaskLookupRestrictionService taskLookupRestrictionService;
    private final TaskVariableLookupRestrictionService taskVariableLookupRestrictionService;

    private final EntityManager entityManager;

    public ActivitiRestrictedKeysProvider(
        EntityManagerFactory entityManagerFactory,
        ProcessDefinitionRestrictionService processDefinitionRestrictionService,
        ProcessInstanceRestrictionService processInstanceRestrictionService,
        ProcessVariableRestrictionService processVariableRestrictionService,
        TaskLookupRestrictionService taskLookupRestrictionService,
        TaskVariableLookupRestrictionService taskVariableLookupRestrictionService
    ) {
        this.processDefinitionRestrictionService = processDefinitionRestrictionService;
        this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
        this.processInstanceRestrictionService = processInstanceRestrictionService;
        this.processVariableRestrictionService = processVariableRestrictionService;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
        this.taskVariableLookupRestrictionService = taskVariableLookupRestrictionService;
    }

    @Override
    public Optional<List<Object>> apply(EntityIntrospector.EntityIntrospectionResult entityDescriptor) {
        var entity = entityDescriptor.getEntity();

        return new ProcessDefinitionRestrictedKeys(entity)
            .get()
            .or(new ProcessInstanceRestrictedKeys(entity))
            .or(new TaskRestrictedKeys(entity))
            .or(new ProcessVariablesRestrictedKeys(entity))
            .or(new TaskVariableRestrictedKeys(entity))
            .or(Optional::empty);
    }

    class ProcessDefinitionRestrictedKeys implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;

        ProcessDefinitionRestrictedKeys(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(ProcessDefinitionEntity.class::equals)
                .map(it -> {
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
                });
        }
    }

    class ProcessInstanceRestrictedKeys implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;

        ProcessInstanceRestrictedKeys(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(ProcessInstanceEntity.class::equals)
                .map(it -> {
                    var predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
                        new BooleanBuilder(),
                        SecurityPolicyAccess.READ
                    );

                    var entity = QProcessInstanceEntity.processInstanceEntity;

                    JPAQuery<?> query = new JPAQuery<QProcessInstanceEntity>(entityManager)
                        .from(entity)
                        .select(entity.id)
                        .where(predicate);

                    return query.fetch().stream().map(Object.class::cast).toList();
                });
        }
    }

    class ProcessVariablesRestrictedKeys implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;

        ProcessVariablesRestrictedKeys(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(ProcessVariableEntity.class::equals)
                .map(it -> {
                    var predicate = processVariableRestrictionService.restrictProcessInstanceVariableQuery(
                        new BooleanBuilder(),
                        SecurityPolicyAccess.READ
                    );

                    var entity = QProcessVariableEntity.processVariableEntity;

                    JPAQuery<?> query = new JPAQuery<QProcessVariableEntity>(entityManager)
                        .from(entity)
                        .select(entity.id)
                        .where(predicate);

                    return query.fetch().stream().map(Object.class::cast).toList();
                });
        }
    }

    class TaskRestrictedKeys implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;

        TaskRestrictedKeys(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(TaskEntity.class::equals)
                .map(it -> {
                    var predicate = taskLookupRestrictionService.restrictTaskQuery(new BooleanBuilder());

                    var entity = QTaskEntity.taskEntity;

                    JPAQuery<?> query = new JPAQuery<QTaskEntity>(entityManager)
                        .from(entity)
                        .select(entity.id)
                        .where(predicate);

                    return query.fetch().stream().map(Object.class::cast).toList();
                });
        }
    }

    class TaskVariableRestrictedKeys implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;

        TaskVariableRestrictedKeys(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(TaskVariableEntity.class::equals)
                .map(it -> {
                    var predicate = taskVariableLookupRestrictionService.restrictTaskVariableQuery(
                        new BooleanBuilder()
                    );

                    var entity = QTaskVariableEntity.taskVariableEntity;

                    JPAQuery<?> query = new JPAQuery<QTaskVariableEntity>(entityManager)
                        .from(entity)
                        .select(entity.id)
                        .where(predicate);

                    return query.fetch().stream().map(Object.class::cast).toList();
                });
        }
    }
}
