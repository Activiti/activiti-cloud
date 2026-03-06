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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.annotation.CountOverFullWindow;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.springframework.util.CollectionUtils;

@CountOverFullWindow
public class ProcessInstanceSpecification
    extends SpecificationSupport<ProcessInstanceEntity, ProcessInstanceSearchRequest> {

    private final String userId;

    private ProcessInstanceSpecification(ProcessInstanceSearchRequest searchRequest, String userId) {
        super(searchRequest);
        this.userId = userId;
    }

    public static ProcessInstanceSpecification unrestricted(ProcessInstanceSearchRequest searchRequest) {
        return new ProcessInstanceSpecification(searchRequest, null);
    }

    public static ProcessInstanceSpecification restricted(ProcessInstanceSearchRequest searchRequest, String userId) {
        return new ProcessInstanceSpecification(searchRequest, userId);
    }

    public static ProcessInstanceSpecification unrestrictedLinkedProcesses(Set<String> linkedProcessInstanceIds) {
        return configureLinkedProcessSpecification(linkedProcessInstanceIds, null);
    }

    public static ProcessInstanceSpecification restrictedLinkedProcesses(
        Set<String> linkedProcessInstanceIds,
        String userId
    ) {
        return configureLinkedProcessSpecification(linkedProcessInstanceIds, userId);
    }

    private static ProcessInstanceSpecification configureLinkedProcessSpecification(
        Set<String> linkedProcessInstanceIds,
        String userId
    ) {
        ProcessInstanceSearchRequest searchRequest = new ProcessInstanceSearchRequest();
        searchRequest.setLinkedProcessInstanceId(linkedProcessInstanceIds);

        return userId == null ? unrestricted(searchRequest) : restricted(searchRequest, userId);
    }

    public static ProcessInstanceSpecification unrestrictedSubprocesses(Set<String> parentIds) {
        return configureSubprocessesSpecification(parentIds, null);
    }

    public static ProcessInstanceSpecification restrictedSubprocesses(Set<String> parentIds, String userId) {
        return configureSubprocessesSpecification(parentIds, userId);
    }

    private static ProcessInstanceSpecification configureSubprocessesSpecification(
        Set<String> parentIds,
        String userId
    ) {
        ProcessInstanceSearchRequest searchRequest = new ProcessInstanceSearchRequest();
        searchRequest.setSubprocessParentIds(parentIds);

        return userId == null ? unrestricted(searchRequest) : restricted(searchRequest, userId);
    }

    @Override
    public Predicate toPredicate(
        Root<ProcessInstanceEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        reset();
        applyUserRestrictionFilter(root, criteriaBuilder);
        applyIdFilter(root);
        applyParentIdFilter(root);
        applyNameFilter(root, criteriaBuilder);
        applyProcessDefinitionNameFilter(root);
        applyInitiatorFilter(root);
        applyAppVersionFilter(root);
        applyStatusFilter(root);
        applyLastModifiedDateFilters(root, criteriaBuilder);
        applyStartFilters(root, criteriaBuilder);
        applyCompletedFilters(root, criteriaBuilder);
        applySuspendedFilters(root, criteriaBuilder);
        applyIncludeSubprocesses(root);
        applyLinkedProcessInstanceId(root);
        applyLinkedProcessInstanceType(root);
        applySubprocessParentIdsFilter(root, criteriaBuilder);
        applyProcessRelatedTo(root, criteriaBuilder);
        applyLinkedAndOrphanProcessesFilter(root, criteriaBuilder);
        return super.toPredicate(root, query, criteriaBuilder);
    }

    private void applyIncludeSubprocesses(Root<ProcessInstanceEntity> root) {
        if (Boolean.FALSE.equals(searchRequest.getIncludeSubprocesses())) {
            predicates.add(root.get(ProcessInstanceEntity_.parentId).isNull());
        }
    }

    @Override
    protected SingularAttribute<ProcessInstanceEntity, String> getIdAttribute() {
        return ProcessInstanceEntity_.id;
    }

    @Override
    protected SetAttribute<ProcessInstanceEntity, ProcessVariableEntity> getProcessVariablesAttribute() {
        return ProcessInstanceEntity_.variables;
    }

    private void applyParentIdFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.parentId())) {
            predicates.add(root.get(ProcessInstanceEntity_.parentId).in(searchRequest.parentId()));
        }
    }

    private void applySubprocessParentIdsFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.getSubprocessParentIds())) {
            Set<String> ids = searchRequest.getSubprocessParentIds();
            predicates.add(
                criteriaBuilder.or(
                    root.get(ProcessInstanceEntity_.parentId).in(ids),
                    root.get(ProcessInstanceEntity_.rootProcessInstanceId).in(ids)
                )
            );
        }
    }

    private void applyLinkedProcessInstanceId(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getLinkedProcessInstanceId())) {
            predicates.add(
                root.get(ProcessInstanceEntity_.linkedProcessInstanceId).in(searchRequest.getLinkedProcessInstanceId())
            );
        }
    }

    private void applyLinkedProcessInstanceType(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getLinkedProcessInstanceType())) {
            predicates.add(
                root
                    .get(ProcessInstanceEntity_.linkedProcessInstanceType)
                    .in(searchRequest.getLinkedProcessInstanceType())
            );
        }
    }

    private void applyNameFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.getName())) {
            addLikeFilters(predicates, searchRequest.getName(), root, criteriaBuilder, ProcessInstanceEntity_.name);
        }
    }

    private void applyProcessDefinitionNameFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getProcessDefinitionName())) {
            predicates.add(
                root.get(ProcessInstanceEntity_.processDefinitionName).in(searchRequest.getProcessDefinitionName())
            );
        }
    }

    private void applyInitiatorFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getInitiator())) {
            predicates.add(root.get(ProcessInstanceEntity_.initiator).in(searchRequest.getInitiator()));
        }
    }

    private void applyAppVersionFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getAppVersion())) {
            predicates.add(root.get(ProcessInstanceEntity_.appVersion).in(searchRequest.getAppVersion()));
        }
    }

    private void applyStatusFilter(Root<ProcessInstanceEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.getStatus())) {
            predicates.add(root.get(ProcessInstanceEntity_.status).in(searchRequest.getStatus()));
        }
    }

    private void applyLastModifiedDateFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.getLastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.lastModified),
                    searchRequest.getLastModifiedFrom()
                )
            );
        }
        if (searchRequest.getLastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(
                    root.get(ProcessInstanceEntity_.lastModified),
                    searchRequest.getLastModifiedTo()
                )
            );
        }
    }

    private void applyStartFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.getStartFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(ProcessInstanceEntity_.startDate), searchRequest.getStartFrom())
            );
        }
        if (searchRequest.getStartTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.startDate), searchRequest.getStartTo())
            );
        }
    }

    private void applyCompletedFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.getCompletedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.completedDate),
                    searchRequest.getCompletedFrom()
                )
            );
        }
        if (searchRequest.getCompletedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.completedDate), searchRequest.getCompletedTo())
            );
        }
    }

    private void applySuspendedFilters(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.getSuspendedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(ProcessInstanceEntity_.suspendedDate),
                    searchRequest.getSuspendedFrom()
                )
            );
        }
        if (searchRequest.getSuspendedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(ProcessInstanceEntity_.suspendedDate), searchRequest.getSuspendedTo())
            );
        }
    }

    private void applyUserRestrictionFilter(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (userId != null) {
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.equal(root.get(ProcessInstanceEntity_.initiator), userId),
                    criteriaBuilder.equal(
                        root.join(ProcessInstanceEntity_.tasks, JoinType.LEFT).get(TaskEntity_.assignee),
                        userId
                    ),
                    criteriaBuilder.equal(
                        root
                            .join(ProcessInstanceEntity_.tasks, JoinType.LEFT)
                            .join(TaskEntity_.taskCandidateUsers, JoinType.LEFT)
                            .get(TaskCandidateUserEntity_.userId),
                        userId
                    )
                )
            );
        }
    }

    private void applyProcessRelatedTo(Root<ProcessInstanceEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.getProcessRelatedTo())) {
            predicates.add(
                criteriaBuilder.or(
                    root.get(ProcessInstanceEntity_.linkedProcessInstanceId).in(searchRequest.getProcessRelatedTo()),
                    root.get(ProcessInstanceEntity_.rootProcessInstanceId).in(searchRequest.getProcessRelatedTo())
                )
            );
        }
    }

    private void applyLinkedAndOrphanProcessesFilter(
        Root<ProcessInstanceEntity> root,
        CriteriaBuilder criteriaBuilder
    ) {
        boolean excludeLinked = Boolean.FALSE.equals(searchRequest.getIncludeLinkedProcesses());
        boolean excludeOrphan = Boolean.FALSE.equals(searchRequest.getIncludeUnlinkedProcesses());

        if (!excludeLinked && !excludeOrphan) {
            return;
        }

        Predicate isLinkedProcess = criteriaBuilder.and(
            root.get(ProcessInstanceEntity_.linkedProcessInstanceId).isNotNull(),
            root.get(ProcessInstanceEntity_.linkedProcessInstanceType).isNotNull()
        );
        Predicate isOrphanProcess = criteriaBuilder.and(
            root.get(ProcessInstanceEntity_.linkedProcessInstanceId).isNull(),
            root.get(ProcessInstanceEntity_.linkedProcessInstanceType).isNotNull()
        );

        if (excludeLinked && excludeOrphan) {
            predicates.add(criteriaBuilder.not(criteriaBuilder.or(isLinkedProcess, isOrphanProcess)));
        } else if (excludeLinked) {
            predicates.add(criteriaBuilder.not(isLinkedProcess));
        } else {
            predicates.add(criteriaBuilder.not(isOrphanProcess));
        }
    }
}
