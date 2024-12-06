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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.services.query.app.repository.annotation.CountOverFullWindow;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity_;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

@CountOverFullWindow
public class TaskSpecification extends SpecificationSupport<TaskEntity> {

    public final TaskSearchRequest taskSearchRequest;

    private final String userId;
    private final Collection<String> userGroups;

    private TaskSpecification(TaskSearchRequest taskSearchRequest, String userId, Collection<String> userGroups) {
        this.taskSearchRequest = taskSearchRequest;
        this.userId = userId;
        this.userGroups = userGroups;
    }

    /**
     * Creates a specification that retrieve tasks that match filters in the request without restrictions related to any user.
     *
     * @param taskSearchRequest the request containing all the filters
     * @return a specification that applies the filters in the request
     */
    public static TaskSpecification unrestricted(TaskSearchRequest taskSearchRequest) {
        return new TaskSpecification(taskSearchRequest, null, null);
    }

    /**
     * Creates a specification that applies the filters and restricts the retrieved tasks based on the given user and groups.
     * In addition to the filters, tasks are retrieved if they match one of the following conditions:
     * - user is assignee
     * - user is owner
     * - user is candidate and task is not assigned
     * - any of the user groups is candidate group and task is not assigned
     * - there are no candidate users and groups set and task is not assigned
     *
     * @param taskSearchRequest the request containing all the filters
     * @param userId            user id to be applied for restriction
     * @param userGroups        groups to be applied for restriction
     * @return a specification that applies the filters and restricts the retrieved tasks based on the given user and groups
     */
    public static TaskSpecification restricted(
        TaskSearchRequest taskSearchRequest,
        String userId,
        Collection<String> userGroups
    ) {
        return new TaskSpecification(taskSearchRequest, userId, userGroups);
    }

    @Override
    public Predicate toPredicate(Root<TaskEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        reset();
        applyUserRestrictionFilter(root, criteriaBuilder);
        applyRootTasksFilter(root, criteriaBuilder);
        applyStandaloneFilter(root, criteriaBuilder);
        applyNameFilter(root, criteriaBuilder);
        applyDescriptionFilter(root, criteriaBuilder);
        applyProcessDefinitionNameFilter(root, criteriaBuilder);
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
        if (!CollectionUtils.isEmpty(taskSearchRequest.taskVariableFilters())) {
            SetJoin<TaskEntity, TaskVariableEntity> tvRoot = root.join(TaskEntity_.variables, JoinType.LEFT);
            List<VariableValueCondition> conditions = taskSearchRequest
                .taskVariableFilters()
                .stream()
                .map(filter -> {
                    VariableValueCondition condition = getCondition(
                        criteriaBuilder.equal(tvRoot.get(TaskVariableEntity_.name), filter.name()),
                        criteriaBuilder,
                        tvRoot.get(TaskVariableEntity_.value),
                        filter
                    );
                    String alias = "tv_" + filter.name();
                    selections.put(alias, condition.getColumnExpression().alias(alias));
                    return condition;
                })
                .toList();
            filterConditions.addAll(conditions);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.processVariableFilters())) {
            SetJoin<TaskEntity, ProcessVariableEntity> pvRoot = joinProcessVariables(
                root,
                TaskEntity_.PROCESS_VARIABLES
            );
            List<VariableValueCondition> conditions = taskSearchRequest
                .processVariableFilters()
                .stream()
                .map(filter -> {
                    VariableValueCondition condition = getCondition(
                        criteriaBuilder.and(
                            criteriaBuilder.equal(
                                pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                                filter.processDefinitionKey()
                            ),
                            criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), filter.name())
                        ),
                        criteriaBuilder,
                        pvRoot.get(ProcessVariableEntity_.value),
                        filter
                    );
                    String alias = getAlias(filter.processDefinitionKey(), filter.name());
                    selections.put(alias, condition.getColumnExpression().alias(alias));
                    return condition;
                })
                .toList();
            filterConditions.addAll(conditions);
        }
        if (!filterConditions.isEmpty()) {
            query.groupBy(root.get(TaskEntity_.id));
            query.having(
                filterConditions
                    .stream()
                    .map(variableValueCondition -> {
                        try {
                            return variableValueCondition.toPredicate();
                        } catch (FunctionArgumentException | IllegalArgumentException e) {
                            throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid filter (type: " +
                                variableValueCondition.getClass().getSimpleName() +
                                ", operator: " +
                                variableValueCondition.operator +
                                ", value: " +
                                variableValueCondition.filterValue +
                                ")"
                            );
                        }
                    })
                    .reduce(criteriaBuilder::and)
                    .orElse(criteriaBuilder.conjunction())
            );
        }
        if (!query.getResultType().equals(Long.class)) {
            applySorting(
                root,
                joinProcessVariables(root, TaskEntity_.PROCESS_VARIABLES),
                taskSearchRequest.sort(),
                query,
                criteriaBuilder
            );
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    private void applyProcessDefinitionNameFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.processDefinitionName())) {
            addLikeFilters(
                predicates,
                taskSearchRequest.processDefinitionName(),
                root,
                criteriaBuilder,
                TaskEntity_.processDefinitionName
            );
        }
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

    private void applyUserRestrictionFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (userId != null) {
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.equal(root.get(TaskEntity_.assignee), userId),
                    criteriaBuilder.equal(root.get(TaskEntity_.owner), userId),
                    criteriaBuilder.and(
                        criteriaBuilder.isNull(root.get(TaskEntity_.assignee)),
                        criteriaBuilder.or(
                            criteriaBuilder.equal(
                                root
                                    .join(TaskEntity_.taskCandidateUsers, JoinType.LEFT)
                                    .get(TaskCandidateUserEntity_.userId),
                                userId
                            ),
                            root
                                .join(TaskEntity_.taskCandidateGroups, JoinType.LEFT)
                                .get(TaskCandidateGroupEntity_.groupId)
                                .in(userGroups),
                            criteriaBuilder.and(
                                criteriaBuilder.isEmpty(root.get(TaskEntity_.taskCandidateUsers)),
                                criteriaBuilder.isEmpty(root.get(TaskEntity_.taskCandidateGroups))
                            )
                        )
                    )
                )
            );
        }
    }
}
