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
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.SingularAttribute;
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
import org.activiti.cloud.services.query.model.dialect.JsonValueFunctions;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public class TaskSpecification implements Specification<TaskEntity> {

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
        if (!CollectionUtils.isEmpty(taskSearchRequest.getCandidateGroupId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateGroups)
                    .get(TaskCandidateGroupEntity_.groupId)
                    .in(taskSearchRequest.getCandidateGroupId())
            );
        }
    }

    private void applyCandidateUserFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getCandidateUserId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateUsers)
                    .get(TaskCandidateUserEntity_.userId)
                    .in(taskSearchRequest.getCandidateUserId())
            );
        }
    }

    private void applyDueDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.getDueDateFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.dueDate), taskSearchRequest.getDueDateFrom())
            );
        }
        if (taskSearchRequest.getDueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.dueDate), taskSearchRequest.getDueDateTo()));
        }
    }

    private void applyCompletedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.getCompletedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.completedDate), taskSearchRequest.getCompletedFrom())
            );
        }
        if (taskSearchRequest.getCompletedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.completedDate), taskSearchRequest.getCompletedTo())
            );
        }
    }

    private void applyLastClaimedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.getLastClaimedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.claimedDate), taskSearchRequest.getLastClaimedFrom())
            );
        }
        if (taskSearchRequest.getLastClaimedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.claimedDate), taskSearchRequest.getLastClaimedTo())
            );
        }
    }

    private void applyLastModifiedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.getLastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.lastModified), taskSearchRequest.getLastModifiedFrom())
            );
        }
        if (taskSearchRequest.getLastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.lastModified), taskSearchRequest.getLastModifiedTo())
            );
        }
    }

    private void applyCreatedDateFilters(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.getCreatedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.createdDate), taskSearchRequest.getCreatedFrom())
            );
        }
        if (taskSearchRequest.getCreatedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.createdDate), taskSearchRequest.getCreatedTo())
            );
        }
    }

    private void applyAssigneeFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getAssignee())) {
            predicates.add(root.get(TaskEntity_.assignee).in(taskSearchRequest.getAssignee()));
        }
    }

    private void applyCompletedByFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getCompletedBy())) {
            predicates.add(root.get(TaskEntity_.completedBy).in(taskSearchRequest.getCompletedBy()));
        }
    }

    private void applyStatusFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getStatus())) {
            predicates.add(root.get(TaskEntity_.status).in(taskSearchRequest.getStatus()));
        }
    }

    private void applyPriorityFilter(Root<TaskEntity> root) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getPriority())) {
            predicates.add(root.get(TaskEntity_.priority).in(taskSearchRequest.getPriority()));
        }
    }

    private void applyDescriptionFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getDescription())) {
            applyLikeFilters(taskSearchRequest.getDescription(), root, criteriaBuilder, TaskEntity_.description);
        }
    }

    private void applyNameFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getName())) {
            applyLikeFilters(taskSearchRequest.getName(), root, criteriaBuilder, TaskEntity_.name);
        }
    }

    private void applyStandaloneFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.isOnlyStandalone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
    }

    private void applyRootTasksFilter(Root<TaskEntity> root, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.isOnlyRoot()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.parentTaskId)));
        }
    }

    private void applyLikeFilters(
        List<String> valuesToFilter,
        Root<TaskEntity> root,
        CriteriaBuilder criteriaBuilder,
        SingularAttribute<TaskEntity, String> attribute
    ) {
        predicates.add(
            valuesToFilter
                .stream()
                .map(value ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(attribute)), "%" + value.toLowerCase() + "%")
                )
                .reduce(criteriaBuilder::or)
                .orElse(criteriaBuilder.conjunction())
        );
    }

    private void applyProcessVariableFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getProcessVariableFilters())) {
            Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get(TaskEntity_.processInstanceId),
                pvRoot.get(ProcessVariableEntity_.processInstanceId)
            );

            Predicate[] variableValueFilters = taskSearchRequest
                .getProcessVariableFilters()
                .stream()
                .map(filter ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(
                            pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                            filter.processDefinitionKey()
                        ),
                        criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), filter.name()),
                        getVariableValueCondition(pvRoot.get(ProcessVariableEntity_.value), filter, criteriaBuilder)
                    )
                )
                .toArray(Predicate[]::new);

            Predicate[] havingClause = taskSearchRequest
                .getProcessVariableFilters()
                .stream()
                .map(filter ->
                    criteriaBuilder.gt(
                        criteriaBuilder.count(
                            criteriaBuilder
                                .selectCase()
                                .when(
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(
                                            pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                                            filter.processDefinitionKey()
                                        ),
                                        criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), filter.name()),
                                        getVariableValueCondition(
                                            pvRoot.get(ProcessVariableEntity_.value),
                                            filter,
                                            criteriaBuilder
                                        )
                                    ),
                                    pvRoot.get(ProcessVariableEntity_.id)
                                )
                                .otherwise(criteriaBuilder.nullLiteral(Long.class))
                        ),
                        criteriaBuilder.literal(0)
                    )
                )
                .toArray(Predicate[]::new);

            query.groupBy(root.get(TaskEntity_.id));
            query.having(havingClause);
            predicates.add(criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters)));
        }
    }

    private void applyTaskVariableFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(taskSearchRequest.getTaskVariableFilters())) {
            SetJoin<TaskEntity, TaskVariableEntity> join = root.join(TaskEntity_.variables);
            Predicate[] variableValueFilters = taskSearchRequest
                .getTaskVariableFilters()
                .stream()
                .map(filter ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(join.get(TaskVariableEntity_.name), filter.name()),
                        getVariableValueCondition(join.get(TaskVariableEntity_.value), filter, criteriaBuilder)
                    )
                )
                .toArray(Predicate[]::new);

            Predicate[] havingClause = taskSearchRequest
                .getTaskVariableFilters()
                .stream()
                .map(filter ->
                    criteriaBuilder.gt(
                        criteriaBuilder.count(
                            criteriaBuilder
                                .selectCase()
                                .when(
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(join.get(TaskVariableEntity_.name), filter.name()),
                                        getVariableValueCondition(
                                            join.get(TaskVariableEntity_.value),
                                            filter,
                                            criteriaBuilder
                                        )
                                    ),
                                    join.get(TaskVariableEntity_.id)
                                )
                                .otherwise(criteriaBuilder.nullLiteral(Long.class))
                        ),
                        criteriaBuilder.literal(0)
                    )
                )
                .toArray(Predicate[]::new);

            query.groupBy(root.get(TaskEntity_.id));
            query.having(havingClause);
            predicates.add(criteriaBuilder.or(variableValueFilters));
        }
    }

    private Predicate getVariableValueCondition(
        Path<?> valueColumnPath,
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder
    ) {
        return switch (filter.operator()) {
            case EQUALS -> criteriaBuilder.isTrue(
                switch (filter.type()) {
                    case BOOLEAN -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Boolean.valueOf(filter.value()))
                    );
                    case INTEGER -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Integer.parseInt(filter.value()))
                    );
                    case STRING, BIGDECIMAL -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATETIME -> criteriaBuilder.function(
                        JsonValueFunctions.DATETIME_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATE -> criteriaBuilder.function(
                        JsonValueFunctions.DATE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                }
            );
            case LIKE -> criteriaBuilder.isTrue(
                criteriaBuilder.function(
                    JsonValueFunctions.LIKE_CASE_INSENSITIVE,
                    Boolean.class,
                    valueColumnPath,
                    criteriaBuilder.literal(filter.value())
                )
            );
            case GREATER_THAN -> criteriaBuilder.isTrue(
                switch (filter.type()) {
                    case INTEGER -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_GREATER_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Integer.parseInt(filter.value()))
                    );
                    case BIGDECIMAL -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_GREATER_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case STRING -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATETIME -> criteriaBuilder.function(
                        JsonValueFunctions.DATETIME_GREATER_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATE -> criteriaBuilder.function(
                        JsonValueFunctions.DATE_GREATER_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    default -> throw new IllegalFilterException(filter);
                }
            );
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.isTrue(
                switch (filter.type()) {
                    case INTEGER -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_GREATER_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Integer.parseInt(filter.value()))
                    );
                    case BIGDECIMAL -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_GREATER_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case STRING -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATETIME -> criteriaBuilder.function(
                        JsonValueFunctions.DATETIME_GREATER_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATE -> criteriaBuilder.function(
                        JsonValueFunctions.DATE_GREATER_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    default -> throw new IllegalFilterException(filter);
                }
            );
            case LESS_THAN -> criteriaBuilder.isTrue(
                switch (filter.type()) {
                    case INTEGER -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_LESS_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Integer.parseInt(filter.value()))
                    );
                    case BIGDECIMAL -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_LESS_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case STRING -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATETIME -> criteriaBuilder.function(
                        JsonValueFunctions.DATETIME_LESS_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATE -> criteriaBuilder.function(
                        JsonValueFunctions.DATE_LESS_THAN,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    default -> throw new IllegalFilterException(filter);
                }
            );
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.isTrue(
                switch (filter.type()) {
                    case INTEGER -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_LESS_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(Integer.parseInt(filter.value()))
                    );
                    case BIGDECIMAL -> criteriaBuilder.function(
                        JsonValueFunctions.NUMERIC_LESS_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case STRING -> criteriaBuilder.function(
                        JsonValueFunctions.VALUE_EQUALS,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATETIME -> criteriaBuilder.function(
                        JsonValueFunctions.DATETIME_LESS_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    case DATE -> criteriaBuilder.function(
                        JsonValueFunctions.DATE_LESS_THAN_EQUAL,
                        Boolean.class,
                        valueColumnPath,
                        criteriaBuilder.literal(filter.value())
                    );
                    default -> throw new IllegalFilterException(filter);
                }
            );
        };
    }
}
