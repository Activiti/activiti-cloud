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
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class ActivitiRestrictedKeysProvider implements RestrictedKeysProvider {

    private final ProcessDefinitionRestrictionService processDefinitionRestrictionService;
    private final ProcessInstanceRestrictionService processInstanceRestrictionService;
    private final ProcessVariableRestrictionService processVariableRestrictionService;
    private final TaskLookupRestrictionService taskLookupRestrictionService;
    private final TaskVariableLookupRestrictionService taskVariableLookupRestrictionService;
    private final EntityManager entityManager;
    private final List<String> unrestrictedRoles;

    private String rolePrefix = "ROLE_";

    public ActivitiRestrictedKeysProvider(
        EntityManagerFactory entityManagerFactory,
        ProcessDefinitionRestrictionService processDefinitionRestrictionService,
        ProcessInstanceRestrictionService processInstanceRestrictionService,
        ProcessVariableRestrictionService processVariableRestrictionService,
        TaskLookupRestrictionService taskLookupRestrictionService,
        TaskVariableLookupRestrictionService taskVariableLookupRestrictionService,
        List<String> unrestrictedRoles
    ) {
        this.processDefinitionRestrictionService = processDefinitionRestrictionService;
        this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
        this.processInstanceRestrictionService = processInstanceRestrictionService;
        this.processVariableRestrictionService = processVariableRestrictionService;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
        this.taskVariableLookupRestrictionService = taskVariableLookupRestrictionService;
        this.unrestrictedRoles = unrestrictedRoles;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    @Override
    public Optional<List<Object>> apply(EntityIntrospector.EntityIntrospectionResult entityDescriptor) {
        var entity = entityDescriptor.getEntity();

        if (isAnonymousUser()) {
            throw new AccessDeniedException("Access denied");
        }

        return ifUnrestrictedByUserRoles()
            .or(new ProcessDefinitionRestrictedKeysSupplier(entity))
            .or(new ProcessInstanceRestrictedKeysSupplier(entity))
            .or(new TaskRestrictedKeysSupplier(entity))
            .or(new ProcessVariablesRestrictedKeysSupplier(entity))
            .or(new TaskVariableRestrictedKeysSupplier(entity));
    }

    boolean isAnonymousUser() {
        return Optional
            .ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(AnonymousAuthenticationToken.class::isInstance)
            .orElse(false);
    }

    Optional<List<Object>> ifUnrestrictedByUserRoles() {
        return Optional
            .ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getAuthorities)
            .map(authorities ->
                authorities
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(value -> value.startsWith(rolePrefix))
                    .map(value -> value.replaceFirst("^".concat(rolePrefix), ""))
                    .anyMatch(unrestrictedRoles::contains)
            )
            .filter(Boolean.TRUE::equals)
            .map(ifUnrestricted -> List.of("*"));
    }

    abstract static class RestrictedKeysSupplier<T> implements Supplier<Optional<List<Object>>> {

        private final Class<?> entityClass;
        private final Class<T> genericType;

        RestrictedKeysSupplier(Class<?> entityClass) {
            this.entityClass = entityClass;
            this.genericType =
                (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }

        @Override
        public Optional<List<Object>> get() {
            return Optional
                .of(entityClass)
                .filter(this::isInstance)
                .map(this::getKeys)
                .filter(Predicate.not(Collection::isEmpty));
        }

        boolean isInstance(Class<?> entityClass) {
            return genericType.equals(this.entityClass);
        }

        abstract List<Object> getKeys(Class<?> entityClass);
    }

    class ProcessDefinitionRestrictedKeysSupplier extends RestrictedKeysSupplier<ProcessDefinitionEntity> {

        ProcessDefinitionRestrictedKeysSupplier(Class<?> entityClass) {
            super(entityClass);
        }

        @Override
        List<Object> getKeys(Class<?> entityClass) {
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
        }
    }

    class ProcessInstanceRestrictedKeysSupplier extends RestrictedKeysSupplier<ProcessInstanceEntity> {

        ProcessInstanceRestrictedKeysSupplier(Class<?> entityClass) {
            super(entityClass);
        }

        @Override
        List<Object> getKeys(Class<?> entityClass) {
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
        }
    }

    class ProcessVariablesRestrictedKeysSupplier extends RestrictedKeysSupplier<ProcessVariableEntity> {

        ProcessVariablesRestrictedKeysSupplier(Class<?> entityClass) {
            super(entityClass);
        }

        @Override
        List<Object> getKeys(Class<?> entityClass) {
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
        }
    }

    class TaskRestrictedKeysSupplier extends RestrictedKeysSupplier<TaskEntity> {

        TaskRestrictedKeysSupplier(Class<?> entityClass) {
            super(entityClass);
        }

        @Override
        List<Object> getKeys(Class<?> entityClass) {
            var predicate = taskLookupRestrictionService.restrictToInvolvedUsersQuery(new BooleanBuilder());

            var taskEntity = QTaskEntity.taskEntity;
            var processInstanceEntity = QProcessInstanceEntity.processInstanceEntity;

            JPAQuery<?> query = new JPAQuery<QTaskEntity>(entityManager)
                .from(taskEntity)
                .leftJoin(processInstanceEntity)
                .on(processInstanceEntity.id.eq(taskEntity.processInstanceId))
                .select(taskEntity.id)
                .where(predicate);

            return query.fetch().stream().map(Object.class::cast).toList();
        }
    }

    class TaskVariableRestrictedKeysSupplier extends RestrictedKeysSupplier<TaskVariableEntity> {

        TaskVariableRestrictedKeysSupplier(Class<?> entityClass) {
            super(entityClass);
        }

        @Override
        List<Object> getKeys(Class<?> entityClass) {
            var predicate = taskVariableLookupRestrictionService.restrictTaskVariableQuery(new BooleanBuilder());

            var entity = QTaskVariableEntity.taskVariableEntity;

            JPAQuery<?> query = new JPAQuery<QTaskVariableEntity>(entityManager)
                .from(entity)
                .select(entity.id)
                .where(predicate);

            return query.fetch().stream().map(Object.class::cast).toList();
        }
    }
}
