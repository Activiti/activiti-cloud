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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity_;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.util.CollectionUtils;

public class TaskSpecification extends SpecificationSupport<TaskEntity> {

    List<Predicate> predicates = new ArrayList<>();

    private final TaskSearchRequest taskSearchRequest;

    public TaskSpecification(TaskSearchRequest taskSearchRequest) {
        this.taskSearchRequest = taskSearchRequest;
    }

    @Override
    public Predicate toPredicate(Root<TaskEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        applyRootTasksFilter(root, criteriaBuilder);
        applyStandaloneFilter(root, criteriaBuilder);
        applyNameFilter(root, criteriaBuilder);
        applyDescriptionFilter(root, criteriaBuilder);
        applyPriorityFilter(root);
        applyStatusFilter(root);
        applyCompletedByFilter(root);
        applyAssigneeFilter(root);
        applyCreatedDateFilters(root, criteriaBuilder);
        applyLastModifiedDateFilters(root, criteriaBuilder);
        applyLastClaimedDateFilters(root, criteriaBuilder);
        applyCompletedDateFilters(root, criteriaBuilder);
        applyDueDateFilters(root, criteriaBuilder);
        applyCandidateUserFilter(root);
        applyCandidateGroupFilter(root);
        applyTaskVariableFilters(root, query, criteriaBuilder);
        applyProcessVariableFilters(root, query, criteriaBuilder);
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    private void applyCandidateGroupFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.candidateGroupId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateGroups)
                    .get(TaskCandidateGroupEntity_.groupId)
                    .in(taskSearchRequest.candidateGroupId())
            );
        }
    }

    private void applyCandidateUserFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.candidateUserId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateUsers)
                    .get(TaskCandidateUserEntity_.userId)
                    .in(taskSearchRequest.candidateUserId())
            );
        }
    }

    private void applyDueDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.dueDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThan(root.get(TaskEntity_.dueDate), taskSearchRequest.dueDateFrom()));
        }
        if (taskSearchRequest.dueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.dueDate), taskSearchRequest.dueDateTo()));
        }
    }

    private void applyCompletedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.completedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.completedDate), taskSearchRequest.completedFrom())
            );
        }
        if (taskSearchRequest.completedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.completedDate), taskSearchRequest.completedTo())
            );
        }
    }

    private void applyLastClaimedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.lastClaimedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.claimedDate), taskSearchRequest.lastClaimedFrom())
            );
        }
        if (taskSearchRequest.lastClaimedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.claimedDate), taskSearchRequest.lastClaimedTo())
            );
        }
    }

    private void applyLastModifiedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.lastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.lastModified), taskSearchRequest.lastModifiedFrom())
            );
        }
        if (taskSearchRequest.lastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.lastModified), taskSearchRequest.lastModifiedTo())
            );
        }
    }

    private void applyCreatedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.createdFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.createdDate), taskSearchRequest.createdFrom())
            );
        }
        if (taskSearchRequest.createdTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.createdDate), taskSearchRequest.createdTo()));
        }
    }

    private void applyAssigneeFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.assignee())) {
            predicates.add(root.get(TaskEntity_.assignee).in(taskSearchRequest.assignee()));
        }
    }

    private void applyCompletedByFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.completedBy())) {
            predicates.add(root.get(TaskEntity_.completedBy).in(taskSearchRequest.completedBy()));
        }
    }

    private void applyStatusFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.status())) {
            predicates.add(root.get(TaskEntity_.status).in(taskSearchRequest.status()));
        }
    }

    private void applyPriorityFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.priority())) {
            predicates.add(root.get(TaskEntity_.priority).in(taskSearchRequest.priority()));
        }
    }

    private void applyDescriptionFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.description())) {
            addLikeFilters(predicates, taskSearchRequest.description(), root, criteriaBuilder, TaskEntity_.description);
        }
    }

    private void applyNameFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.name())) {
            addLikeFilters(predicates, taskSearchRequest.name(), root, criteriaBuilder, TaskEntity_.name);
        }
    }

    private void applyStandaloneFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.onlyStandalone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
    }

    private void applyRootTasksFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.onlyRoot()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.parentTaskId)));
        }
    }

    private void applyProcessVariableFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.processVariableFilters())) {
            Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get(TaskEntity_.processInstanceId),
                pvRoot.get(ProcessVariableEntity_.processInstanceId)
            );

            query.groupBy(root.get(TaskEntity_.id));
            query.having(
                criteriaBuilder.equal(
                    criteriaBuilder.countDistinct(pvRoot.get(ProcessVariableEntity_.name)),
                    criteriaBuilder.literal(taskSearchRequest.processVariableFilters().size())
                )
            );
            predicates.add(
                criteriaBuilder.and(
                    joinCondition,
                    criteriaBuilder.or(
                        getProcessVariableValueFilters(
                            pvRoot,
                            taskSearchRequest.processVariableFilters(),
                            criteriaBuilder
                        )
                    )
                )
            );
        }
    }

    private void applyTaskVariableFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.taskVariableFilters())) {
            SetJoin<TaskEntity, TaskVariableEntity> join = root.join(TaskEntity_.variables);
            Predicate[] variableValueFilters = taskSearchRequest
                .taskVariableFilters()
                .stream()
                .map(filter ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(join.get(TaskVariableEntity_.name), filter.name()),
                        getVariableValueCondition(join.get(TaskVariableEntity_.value), filter, criteriaBuilder)
                    )
                )
                .toArray(Predicate[]::new);

            query.groupBy(root.get(TaskEntity_.id));
            query.having(
                criteriaBuilder.equal(
                    criteriaBuilder.countDistinct(join.get(ProcessVariableEntity_.name)),
                    criteriaBuilder.literal(taskSearchRequest.taskVariableFilters().size())
                )
            );
            predicates.add(criteriaBuilder.or(variableValueFilters));
        }
    }
}
