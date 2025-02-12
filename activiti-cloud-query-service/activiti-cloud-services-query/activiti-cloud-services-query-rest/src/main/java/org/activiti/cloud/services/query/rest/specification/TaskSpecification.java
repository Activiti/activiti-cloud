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
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.Map;
import org.activiti.cloud.services.query.app.repository.annotation.CountOverFullWindow;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity_;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity_;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.util.CollectionUtils;

@CountOverFullWindow
public class TaskSpecification extends SpecificationSupport<TaskEntity, TaskSearchRequest> {

    private final String userId;
    private final Collection<String> userGroups;

    private TaskSpecification(TaskSearchRequest searchRequest, String userId, Collection<String> userGroups) {
        super(searchRequest);
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
        applyIdFilter(root);
        applyParentIdFilter(root);
        applyProcessInstanceIdFilter(root);
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
        if (!CollectionUtils.isEmpty(searchRequest.taskVariableFilters())) {
            SetJoin<TaskEntity, TaskVariableEntity> tvRoot = root.join(TaskEntity_.variables, JoinType.LEFT);
            filterConditions.addAll(
                searchRequest
                    .taskVariableFilters()
                    .stream()
                    .map(filter ->
                        new VariableValueFilterConditionImpl<>(
                            (SetJoin<TaskEntity, ? extends AbstractVariableEntity>) tvRoot,
                            Map.of(tvRoot.get(TaskVariableEntity_.name), filter.name()),
                            javaTypeMapping.get(filter.type()),
                            filter,
                            criteriaBuilder
                        )
                    )
                    .toList()
            );
        }
        return super.toPredicate(root, query, criteriaBuilder);
    }

    @Override
    protected SingularAttribute<TaskEntity, String> getIdAttribute() {
        return TaskEntity_.id;
    }

    @Override
    protected SetAttribute<TaskEntity, ProcessVariableEntity> getProcessVariablesAttribute() {
        return TaskEntity_.processVariables;
    }

    private void applyParentIdFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.parentId())) {
            predicates.add(root.get(TaskEntity_.parentTaskId).in(searchRequest.parentId()));
        }
    }

    private void applyProcessInstanceIdFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.processInstanceId())) {
            predicates.add(root.get(TaskEntity_.processInstanceId).in(searchRequest.processInstanceId()));
        }
    }

    private void applyProcessDefinitionNameFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.processDefinitionName())) {
            addLikeFilters(
                predicates,
                searchRequest.processDefinitionName(),
                root,
                criteriaBuilder,
                TaskEntity_.processDefinitionName
            );
        }
    }

    private void applyCandidateGroupFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.candidateGroupId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateGroups)
                    .get(TaskCandidateGroupEntity_.groupId)
                    .in(searchRequest.candidateGroupId())
            );
        }
    }

    private void applyCandidateUserFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.candidateUserId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateUsers)
                    .get(TaskCandidateUserEntity_.userId)
                    .in(searchRequest.candidateUserId())
            );
        }
    }

    private void applyDueDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.dueDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThan(root.get(TaskEntity_.dueDate), searchRequest.dueDateFrom()));
        }
        if (searchRequest.dueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.dueDate), searchRequest.dueDateTo()));
        }
    }

    private void applyCompletedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.completedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.completedDate), searchRequest.completedFrom())
            );
        }
        if (searchRequest.completedTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.completedDate), searchRequest.completedTo()));
        }
    }

    private void applyLastClaimedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.lastClaimedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.claimedDate), searchRequest.lastClaimedFrom())
            );
        }
        if (searchRequest.lastClaimedTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.claimedDate), searchRequest.lastClaimedTo()));
        }
    }

    private void applyLastModifiedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.lastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.lastModified), searchRequest.lastModifiedFrom())
            );
        }
        if (searchRequest.lastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.lastModified), searchRequest.lastModifiedTo())
            );
        }
    }

    private void applyCreatedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.createdFrom() != null) {
            predicates.add(criteriaBuilder.greaterThan(root.get(TaskEntity_.createdDate), searchRequest.createdFrom()));
        }
        if (searchRequest.createdTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.createdDate), searchRequest.createdTo()));
        }
    }

    private void applyAssigneeFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.assignee())) {
            predicates.add(root.get(TaskEntity_.assignee).in(searchRequest.assignee()));
        }
    }

    private void applyCompletedByFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.completedBy())) {
            predicates.add(root.get(TaskEntity_.completedBy).in(searchRequest.completedBy()));
        }
    }

    private void applyStatusFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.status())) {
            predicates.add(root.get(TaskEntity_.status).in(searchRequest.status()));
        }
    }

    private void applyPriorityFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(searchRequest.priority())) {
            predicates.add(root.get(TaskEntity_.priority).in(searchRequest.priority()));
        }
    }

    private void applyDescriptionFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.description())) {
            addLikeFilters(predicates, searchRequest.description(), root, criteriaBuilder, TaskEntity_.description);
        }
    }

    private void applyNameFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(searchRequest.name())) {
            addLikeFilters(predicates, searchRequest.name(), root, criteriaBuilder, TaskEntity_.name);
        }
    }

    private void applyStandaloneFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.onlyStandalone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
    }

    private void applyRootTasksFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (searchRequest.onlyRoot()) {
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
