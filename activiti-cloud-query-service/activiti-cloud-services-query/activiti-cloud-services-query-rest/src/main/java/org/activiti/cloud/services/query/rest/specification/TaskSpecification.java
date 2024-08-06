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
        if (taskSearchRequest.onlyRoot()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.parentTaskId)));
        }
        if (taskSearchRequest.onlyStandalone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.name())) {
            applyLikeFilters(taskSearchRequest.name(), criteriaBuilder, root, TaskEntity_.name);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.description())) {
            applyLikeFilters(taskSearchRequest.description(), criteriaBuilder, root, TaskEntity_.description);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.priority())) {
            predicates.add(root.get(TaskEntity_.priority).in(taskSearchRequest.priority()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.status())) {
            predicates.add(root.get(TaskEntity_.status).in(taskSearchRequest.status()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.completedBy())) {
            predicates.add(root.get(TaskEntity_.completedBy).in(taskSearchRequest.completedBy()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.assignee())) {
            predicates.add(root.get(TaskEntity_.assignee).in(taskSearchRequest.assignee()));
        }
        if (taskSearchRequest.createdFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.createdDate), taskSearchRequest.createdFrom())
            );
        }
        if (taskSearchRequest.createdTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.createdDate), taskSearchRequest.createdTo()));
        }
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
        if (taskSearchRequest.dueDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThan(root.get(TaskEntity_.dueDate), taskSearchRequest.dueDateFrom()));
        }
        if (taskSearchRequest.dueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(TaskEntity_.dueDate), taskSearchRequest.dueDateTo()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.candidateUserId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateUsers)
                    .get(TaskCandidateUserEntity_.userId)
                    .in(taskSearchRequest.candidateUserId())
            );
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.candidateGroupId())) {
            predicates.add(
                root
                    .join(TaskEntity_.taskCandidateGroups)
                    .get(TaskCandidateGroupEntity_.groupId)
                    .in(taskSearchRequest.candidateGroupId())
            );
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.taskVariableFilters())) {
            applyTaskVariableFilters(root, query, criteriaBuilder);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.processVariableFilters())) {
            applyProcessVariableFilters(root, query, criteriaBuilder);
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    private void applyLikeFilters(
        List<String> valuesToFilter,
        CriteriaBuilder criteriaBuilder,
        Root<TaskEntity> root,
        SingularAttribute<TaskEntity, String> attribute
    ) {
        predicates.add(
            valuesToFilter
                .stream()
                .map(value ->
                    criteriaBuilder.isTrue(
                        criteriaBuilder.function(
                            "sql",
                            Boolean.class,
                            criteriaBuilder.literal(attribute.getName() + " ilike '%" + value + "%'")
                        )
                    )
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
        Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
        Predicate joinCondition = criteriaBuilder.equal(
            root.get(TaskEntity_.processInstanceId),
            pvRoot.get(ProcessVariableEntity_.processInstanceId)
        );

        Predicate[] variableValueFilters = taskSearchRequest
            .processVariableFilters()
            .stream()
            .map(filter ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(
                        pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                        filter.processDefinitionKey()
                    ),
                    criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), filter.name()),
                    getVariableValueCondition(filter, criteriaBuilder)
                )
            )
            .toArray(Predicate[]::new);

        Predicate[] havingClause = taskSearchRequest
            .processVariableFilters()
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
                                    getVariableValueCondition(filter, criteriaBuilder)
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

    private void applyTaskVariableFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        SetJoin<TaskEntity, TaskVariableEntity> join = root.join(TaskEntity_.variables);
        Predicate[] variableValueFilters = taskSearchRequest
            .taskVariableFilters()
            .stream()
            .map(filter ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(join.get(TaskVariableEntity_.name), filter.name()),
                    getVariableValueCondition(filter, criteriaBuilder)
                )
            )
            .toArray(Predicate[]::new);

        Predicate[] havingClause = taskSearchRequest
            .taskVariableFilters()
            .stream()
            .map(filter ->
                criteriaBuilder.gt(
                    criteriaBuilder.count(
                        criteriaBuilder
                            .selectCase()
                            .when(
                                criteriaBuilder.and(
                                    criteriaBuilder.equal(join.get(TaskVariableEntity_.name), filter.name()),
                                    getVariableValueCondition(filter, criteriaBuilder)
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

    private static Predicate getVariableValueCondition(VariableFilter filter, CriteriaBuilder criteriaBuilder) {
        return switch (filter.operator()) {
            case EQUALS -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER, BOOLEAN -> ProcessVariableEntity_.VALUE +
                        " @@ '$.value == " +
                        filter.value() +
                        "'";
                        case STRING, BIGDECIMAL -> ProcessVariableEntity_.VALUE +
                        " @@ '$.value == \"" +
                        filter.value() +
                        "\"'";
                        case DATETIME -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::TIMESTAMPTZ = '" +
                        filter.value() +
                        "'::TIMESTAMPTZ";
                        case DATE -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::DATE = '" +
                        filter.value() +
                        "'::DATE";
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case CONTAINS -> criteriaBuilder.isTrue(
                criteriaBuilder.function(
                    "sql",
                    Boolean.class,
                    criteriaBuilder.literal(
                        ProcessVariableEntity_.VALUE + " @@ '$.value like_regex \"(?i).*" + filter.value() + ".*\"'"
                    )
                )
            );
            case GREATER_THAN -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> ProcessVariableEntity_.VALUE + " @@ '$.value > " + filter.value() + "'";
                        case BIGDECIMAL -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::NUMERIC > " +
                        filter.value();
                        case STRING -> ProcessVariableEntity_.VALUE + " @@ '$.value > \"" + filter.value() + "\"'";
                        case DATETIME -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::TIMESTAMPTZ > '" +
                        filter.value() +
                        "'::TIMESTAMPTZ";
                        case DATE -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::DATE > '" +
                        filter.value() +
                        "'::DATE";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case GREATER_THAN_OR_EQUAL -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> ProcessVariableEntity_.VALUE + " @@ '$.value >= " + filter.value() + "'";
                        case BIGDECIMAL -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::NUMERIC >= " +
                        filter.value();
                        case STRING -> ProcessVariableEntity_.VALUE + " @@ '$.value >= \"" + filter.value() + "\"'";
                        case DATETIME -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::TIMESTAMPTZ >= '" +
                        filter.value() +
                        "'::TIMESTAMPTZ";
                        case DATE -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::DATE >= '" +
                        filter.value() +
                        "'::DATE";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case LESS_THAN -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> ProcessVariableEntity_.VALUE + " @@ '$.value < " + filter.value() + "'";
                        case BIGDECIMAL -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::NUMERIC < " +
                        filter.value();
                        case STRING -> ProcessVariableEntity_.VALUE + " @@ '$.value < \"" + filter.value() + "\"'";
                        case DATETIME -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::TIMESTAMPTZ < '" +
                        filter.value() +
                        "'::TIMESTAMPTZ";
                        case DATE -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::DATE < '" +
                        filter.value() +
                        "'::DATE";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case LESS_THAN_OR_EQUAL -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> ProcessVariableEntity_.VALUE + " @@ '$.value <= " + filter.value() + "'";
                        case BIGDECIMAL -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::NUMERIC <= " +
                        filter.value();
                        case STRING -> ProcessVariableEntity_.VALUE + " @@ '$.value <= \"" + filter.value() + "\"'";
                        case DATETIME -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::TIMESTAMPTZ <= '" +
                        filter.value() +
                        "'::TIMESTAMPTZ";
                        case DATE -> "(" +
                        ProcessVariableEntity_.VALUE +
                        "->>'value')::DATE <= '" +
                        filter.value() +
                        "'::DATE";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
        };
    }
}
