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

package org.activiti.cloud.services.query.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ProcessInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceService.class);

    private final ProcessInstanceRepository processInstanceRepository;

    private final TaskRepository taskRepository;

    private final ProcessInstanceRestrictionService processInstanceRestrictionService;

    private final SecurityPoliciesManager securityPoliciesApplicationService;

    private final SecurityManager securityManager;

    private final EntityFinder entityFinder;

    @PersistenceContext
    private EntityManager entityManager;

    public ProcessInstanceService(
        ProcessInstanceRepository processInstanceRepository,
        TaskRepository taskRepository,
        ProcessInstanceRestrictionService processInstanceRestrictionService,
        SecurityPoliciesManager securityPoliciesApplicationService,
        SecurityManager securityManager,
        EntityFinder entityFinder
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.processInstanceRestrictionService = processInstanceRestrictionService;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
        this.securityManager = securityManager;
        this.entityFinder = entityFinder;
    }

    public Page<ProcessInstanceEntity> findAll(Predicate predicate, Pageable pageable) {
        Predicate transformedPredicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new),
            SecurityPolicyAccess.READ
        );

        return processInstanceRepository.findAll(transformedPredicate, pageable);
    }

    @Transactional
    public Page<ProcessInstanceEntity> findAllWithVariables(
        Predicate predicate,
        List<String> variableKeys,
        Pageable pageable
    ) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("variablesFilter");
        filter.setParameterList("variableKeys", variableKeys);
        Page<ProcessInstanceEntity> processInstanceEntities = findAll(predicate, pageable);
        var ids = processInstanceEntities.map(ProcessInstanceEntity::getId).toList();
        var result = processInstanceRepository.findByIdIsIn(ids, pageable.getSort());

        return new PageImpl<>(result, pageable, processInstanceEntities.getTotalElements());
    }

    public ProcessInstanceEntity findById(String processInstanceId) {
        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(
            processInstanceRepository,
            processInstanceId,
            String.format("Unable to find process instance for the given id:'%s'", processInstanceId)
        );

        if (!canRead(processInstanceEntity)) {
            LOGGER.debug(
                String.format(
                    "User %s not permitted to access definition %s and/or process instance id %s",
                    securityManager.getAuthenticatedUserId(),
                    processInstanceEntity.getProcessDefinitionKey(),
                    processInstanceId
                )
            );
            throw new ActivitiForbiddenException(
                String.format(
                    "Operation not permitted for %s and/or process instance",
                    processInstanceEntity.getProcessDefinitionKey()
                )
            );
        }
        return processInstanceEntity;
    }

    public Page<ProcessInstanceEntity> subprocesses(String processInstanceId, Predicate predicate, Pageable pageable) {
        Predicate transformedPredicate = Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new);

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(
            processInstanceRepository,
            processInstanceId,
            "Unable to find process for the given id:'" + processInstanceId + "'"
        );

        if (!canRead(processInstanceEntity)) {
            LOGGER.debug(
                "User " +
                securityManager.getAuthenticatedUserId() +
                " not permitted to access definition " +
                processInstanceEntity.getProcessDefinitionKey() +
                " and/or process instance id " +
                processInstanceId
            );
            throw new ActivitiForbiddenException(
                "Operation not permitted for " +
                processInstanceEntity.getProcessDefinitionKey() +
                " and/or process instance"
            );
        }

        QProcessInstanceEntity process = QProcessInstanceEntity.processInstanceEntity;
        BooleanExpression expression = process.parentId.eq(processInstanceId);
        Predicate extendedPredicate = expression.and(transformedPredicate);

        return processInstanceRepository.findAll(extendedPredicate, pageable);
    }

    private boolean canRead(ProcessInstanceEntity processInstanceEntity) {
        return (
            securityPoliciesApplicationService.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            ) &&
            (
                securityManager.getAuthenticatedUserId().equals(processInstanceEntity.getInitiator()) ||
                isInvolvedInATask(processInstanceEntity.getId())
            )
        );
    }

    private boolean isInvolvedInATask(String processInstanceId) {
        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        List<String> authenticatedUserGroups = securityManager.getAuthenticatedUserGroups();

        QTaskEntity taskEntity = QTaskEntity.taskEntity;

        BooleanExpression taskInvolved = taskEntity.assignee
            .eq(authenticatedUserId)
            .or(taskEntity.owner.eq(authenticatedUserId))
            .or(taskEntity.taskCandidateUsers.any().userId.eq(authenticatedUserId));

        if (authenticatedUserGroups != null && authenticatedUserGroups.size() > 0) {
            taskInvolved = taskInvolved.or(taskEntity.taskCandidateGroups.any().groupId.in(authenticatedUserGroups));
        }

        Predicate whereExpression = taskEntity.processInstanceId.eq(processInstanceId).and(taskInvolved);

        return taskRepository.exists(whereExpression);
    }
}
