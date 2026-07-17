/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification;
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
import org.springframework.transaction.annotation.Transactional;

public class ProcessInstanceService {

    private static final String UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID = "Unable to find process for the given id:'";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceService.class);

    private static final String ADMIN_ROLE = "ACTIVITI_ADMIN";

    private final ProcessInstanceRepository processInstanceRepository;

    private final TaskRepository taskRepository;

    private final ProcessInstanceSearchService processInstanceSearchService;

    private final ProcessInstanceRestrictionService processInstanceRestrictionService;

    private final SecurityPoliciesManager securityPoliciesApplicationService;

    private final SecurityManager securityManager;

    private final EntityFinder entityFinder;

    @PersistenceContext
    private EntityManager entityManager;

    public ProcessInstanceService(
        ProcessInstanceRepository processInstanceRepository,
        TaskRepository taskRepository,
        ProcessInstanceSearchService processInstanceSearchService,
        ProcessInstanceRestrictionService processInstanceRestrictionService,
        SecurityPoliciesManager securityPoliciesApplicationService,
        SecurityManager securityManager,
        EntityFinder entityFinder
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.processInstanceSearchService = processInstanceSearchService;
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
            UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID + processInstanceId + "'"
        );

        if (!canReadOrAdmin(processInstanceEntity)) {
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

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> search(ProcessInstanceSearchRequest searchRequest, Pageable pageable) {
        return processInstanceSearchService.searchRestricted(searchRequest, pageable);
    }

    public boolean canReadOrAdmin(ProcessInstanceEntity processInstanceEntity) {
        return canRead(processInstanceEntity) || securityManager.getAuthenticatedUserRoles().contains(ADMIN_ROLE);
    }

    private boolean canRead(ProcessInstanceEntity processInstanceEntity) {
        return (
            securityPoliciesApplicationService.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            ) &&
            (securityManager.getAuthenticatedUserId().equals(processInstanceEntity.getInitiator()) ||
                isInvolvedInATask(processInstanceEntity.getId()))
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

    @Transactional(readOnly = true)
    public Long count(ProcessInstanceSearchRequest searchRequest) {
        return processInstanceSearchService.countRestricted(searchRequest);
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchLinkedProcesses(String linkedProcessInstanceId, Pageable pageable) {
        entityFinder.findById(
            processInstanceRepository,
            linkedProcessInstanceId,
            UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID + linkedProcessInstanceId + "'"
        );

        String userId = securityManager.getAuthenticatedUserId();
        ProcessInstanceSpecification restrictedSpecification = ProcessInstanceSpecification.restrictedLinkedProcesses(
            Set.of(linkedProcessInstanceId),
            userId
        );

        return processInstanceRepository.findAll(restrictedSpecification, pageable);
    }

    @Transactional(readOnly = true)
    public List<ProcessInstanceEntity> searchLinkedProcesses(Set<String> linkedProcessInstanceIds) {
        if (linkedProcessInstanceIds == null || linkedProcessInstanceIds.isEmpty()) {
            return List.of();
        }

        String userId = securityManager.getAuthenticatedUserId();
        ProcessInstanceSpecification restrictedSpecification = ProcessInstanceSpecification.restrictedLinkedProcesses(
            linkedProcessInstanceIds,
            userId
        );

        return processInstanceRepository.findAll(restrictedSpecification);
    }

    public void linkProcessInstances(
        String mainProcessInstanceId,
        List<String> processInstanceIds,
        String linkProcessInstanceType
    ) {
        var mainProcess = entityFinder.findById(
            processInstanceRepository,
            mainProcessInstanceId,
            UNABLE_TO_FIND_PROCESS_FOR_THE_GIVEN_ID + mainProcessInstanceId + "'"
        );

        if (processInstanceIds != null && !processInstanceIds.isEmpty()) {
            var request = new ProcessInstanceSearchRequest();
            request.setId(new HashSet<>(processInstanceIds));
            request.setLinkedProcessInstanceType(Set.of(linkProcessInstanceType));

            var orphanProcesses = processInstanceRepository.findAll(
                ProcessInstanceSpecification.restricted(request, securityManager.getAuthenticatedUserId())
            );

            if (orphanProcesses.isEmpty()) {
                LOGGER.debug("No process instance found for the given ids:'{}'", processInstanceIds);
                return;
            }

            orphanProcesses.forEach(orphanProcess -> {
                if (orphanProcess.getInitiator().equals(mainProcess.getInitiator())) {
                    orphanProcess.setLinkedProcessInstanceId(mainProcess.getId());
                    processInstanceRepository.save(orphanProcess);
                } else {
                    LOGGER.debug(
                        "User {} not permitted to link process instance id {} to main process instance id {}",
                        securityManager.getAuthenticatedUserId(),
                        orphanProcess.getId(),
                        mainProcessInstanceId
                    );
                }
            });
        } else {
            LOGGER.debug(
                "No process instance id provided to link to main process instance id '{}'",
                mainProcessInstanceId
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<ProcessInstanceEntity> searchSubProcesses(Page<ProcessInstanceEntity> processInstances) {
        List<ProcessInstanceEntity> content = processInstances.getContent();

        if (content.isEmpty()) {
            return processInstances;
        }

        Set<String> ids = content.stream().map(ProcessInstanceEntity::getId).collect(Collectors.toSet());

        String userId = securityManager.getAuthenticatedUserId();
        // Single query: direct children via parentId, grandchildren+ via rootProcessInstanceId
        List<ProcessInstanceEntity> allDescendants = processInstanceRepository.findAll(
            ProcessInstanceSpecification.restrictedSubprocesses(ids, userId)
        );

        // parentId → children map (covers all levels)
        Map<String, Set<QueryCloudSubprocessInstance>> childrenByParentId = allDescendants
            .stream()
            .filter(pi -> pi.getParentId() != null)
            .collect(
                Collectors.groupingBy(
                    ProcessInstanceEntity::getParentId,
                    Collectors.mapping(this::getQueryCloudSubprocessInstance, Collectors.toSet())
                )
            );

        // For each page entity, collect only its direct children (grandchildren are nested inside them)
        content.forEach(pi -> pi.setSubprocesses(childrenByParentId.getOrDefault(pi.getId(), Set.of())));

        return processInstances;
    }

    private QueryCloudSubprocessInstance getQueryCloudSubprocessInstance(ProcessInstanceEntity subprocess) {
        QueryCloudSubprocessInstance subProcessInstance = new QueryCloudSubprocessInstance();
        subProcessInstance.setId(subprocess.getId());
        subProcessInstance.setProcessDefinitionName(subprocess.getProcessDefinitionName());
        return subProcessInstance;
    }
}
