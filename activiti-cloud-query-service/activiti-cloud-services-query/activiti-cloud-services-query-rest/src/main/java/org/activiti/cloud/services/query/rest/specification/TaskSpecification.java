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
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity_;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public class TaskSpecification implements Specification<TaskEntity> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart()
        .appendOffset("+HH:MM", "+00:00")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "+0000")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH", "Z")
        .optionalEnd()
        .toFormatter();

    List<Predicate> predicates = new ArrayList<>();

    private final TaskSearchRequest taskSearchRequest;

    public TaskSpecification(TaskSearchRequest taskSearchRequest) {
        this.taskSearchRequest = taskSearchRequest;
    }

    @Override
    public Predicate toPredicate(Root<TaskEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (taskSearchRequest.rootTasksOnly()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.parentTaskId)));
        }
        if (taskSearchRequest.standAlone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.name())) {
            applyLikeFilters(taskSearchRequest.name(), criteriaBuilder, root, TaskEntity_.name);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.description())) {
            applyLikeFilters(taskSearchRequest.description(), criteriaBuilder, root, TaskEntity_.description);
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.processDefinitionName())) {
            applyLikeFilters(
                taskSearchRequest.processDefinitionName(),
                criteriaBuilder,
                root,
                TaskEntity_.processDefinitionName
            );
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.priority())) {
            predicates.add(root.get(TaskEntity_.priority).in(taskSearchRequest.priority()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.status())) {
            predicates.add(
                root.get(TaskEntity_.status).in(taskSearchRequest.status().stream().map(String::toUpperCase))
            );
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.completedBy())) {
            predicates.add(root.get(TaskEntity_.completedBy).in(taskSearchRequest.completedBy()));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.assignee())) {
            predicates.add(root.get(TaskEntity_.assignee).in(taskSearchRequest.assignee()));
        }
        if (taskSearchRequest.createdFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(TaskEntity_.createdDate),
                    parseDate(taskSearchRequest.createdFrom())
                )
            );
        }
        if (taskSearchRequest.createdTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.createdDate), parseDate(taskSearchRequest.createdTo()))
            );
        }
        if (taskSearchRequest.lastModifiedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(TaskEntity_.lastModified),
                    parseDate(taskSearchRequest.lastModifiedFrom())
                )
            );
        }
        if (taskSearchRequest.lastModifiedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(
                    root.get(TaskEntity_.lastModified),
                    parseDate(taskSearchRequest.lastModifiedTo())
                )
            );
        }
        if (taskSearchRequest.lastClaimedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(TaskEntity_.claimedDate),
                    parseDate(taskSearchRequest.lastClaimedFrom())
                )
            );
        }
        if (taskSearchRequest.lastClaimedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(
                    root.get(TaskEntity_.claimedDate),
                    parseDate(taskSearchRequest.lastClaimedTo())
                )
            );
        }
        if (taskSearchRequest.completedFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(
                    root.get(TaskEntity_.completedDate),
                    parseDate(taskSearchRequest.completedFrom())
                )
            );
        }
        if (taskSearchRequest.completedTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(
                    root.get(TaskEntity_.completedDate),
                    parseDate(taskSearchRequest.completedTo())
                )
            );
        }
        if (taskSearchRequest.dueDateFrom() != null) {
            predicates.add(
                criteriaBuilder.greaterThan(root.get(TaskEntity_.dueDate), parseDate(taskSearchRequest.dueDateFrom()))
            );
        }
        if (taskSearchRequest.dueDateTo() != null) {
            predicates.add(
                criteriaBuilder.lessThan(root.get(TaskEntity_.dueDate), parseDate(taskSearchRequest.dueDateTo()))
            );
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.candidateGroupId())) {
            if (taskSearchRequest.candidateGroupId().size() > 1) {
                predicates.add(
                    root
                        .join(TaskEntity_.taskCandidateGroups)
                        .get(TaskCandidateGroupEntity_.groupId)
                        .in(taskSearchRequest.candidateGroupId())
                );
            }
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.processVariableFilters())) {
            applyProcessVariableFilters(root, query, criteriaBuilder);
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    private Date parseDate(String stringDate) {
        return Date.from(Instant.from(DATE_TIME_FORMATTER.parse(stringDate)));
    }

    private void applyLikeFilters(
        List<String> valuesToFilter,
        CriteriaBuilder criteriaBuilder,
        Root<TaskEntity> root,
        SingularAttribute<TaskEntity, String> attribute
    ) {
        if (valuesToFilter.size() > 1) {
            predicates.add(
                criteriaBuilder.or(
                    valuesToFilter
                        .stream()
                        .map(value -> criteriaBuilder.like(root.get(attribute), "%" + value + "%"))
                        .toArray(Predicate[]::new)
                )
            );
        } else {
            predicates.add(criteriaBuilder.like(root.get(attribute), "%" + valuesToFilter.getFirst() + "%"));
        }
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
                    getProcessDefinitionCondition(filter, criteriaBuilder, pvRoot),
                    getVariableNameCondition(filter, criteriaBuilder, pvRoot),
                    getVariableValueCondition(filter, criteriaBuilder, pvRoot)
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
                                    getProcessDefinitionCondition(filter, criteriaBuilder, pvRoot),
                                    getVariableNameCondition(filter, criteriaBuilder, pvRoot),
                                    getVariableValueCondition(filter, criteriaBuilder, pvRoot)
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

    private static Predicate getVariableNameCondition(
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("name"), filter.name());
    }

    private static Predicate getProcessDefinitionCondition(
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), filter.processDefinitionKey());
    }

    private static Predicate getVariableValueCondition(
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return switch (filter.operator()) {
            case EQUALS -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER, BOOLEAN -> "value @@ '$.value == " + filter.value() + "'";
                        case STRING, BIGDECIMAL, DATETIME -> "value @@ '$.value == \"" + filter.value() + "\"'";
                        case DATE -> "value @@ '$.value::DATE == \"" + filter.value() + "\"'";
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case CONTAINS -> criteriaBuilder.isTrue(
                criteriaBuilder.function(
                    "sql",
                    Boolean.class,
                    criteriaBuilder.literal("value @@ '$.value LIKE \"%" + filter.value() + "%\"'")
                )
            );
            case GREATER_THAN -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> "value @@ '$.value > " + filter.value() + "'";
                        case BIGDECIMAL -> "value @@ '$.value::NUMERIC > " + filter.value() + "'";
                        case STRING -> "value @@ '$.value > \"" + filter.value() + "\"'";
                        case DATETIME -> "value @@ '$.value::TIMESTAMPTZ > \"" + filter.value() + "\"'";
                        case DATE -> "value @@ '$.value::DATE > \"" + filter.value() + "\"'";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case GREATER_THAN_OR_EQUALS -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> "value @@ '$.value >= " + filter.value() + "'";
                        case BIGDECIMAL -> "value @@ '$.value::NUMERIC >= " + filter.value() + "'";
                        case STRING -> "value @@ '$.value >= \"" + filter.value() + "\"'";
                        case DATETIME -> "value @@ '$.value::TIMESTAMPTZ >= \"" + filter.value() + "\"'";
                        case DATE -> "value @@ '$.value::DATE >= \"" + filter.value() + "\"'";
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
                        case INTEGER -> "value @@ '$.value < " + filter.value() + "'";
                        case BIGDECIMAL -> "value @@ '$.value::NUMERIC < " + filter.value() + "'";
                        case STRING -> "value @@ '$.value < \"" + filter.value() + "\"'";
                        case DATETIME -> "value @@ '$.value::TIMESTAMPTZ < \"" + filter.value() + "\"'";
                        case DATE -> "value @@ '$.value::DATE < \"" + filter.value() + "\"'";
                        default -> throw new IllegalArgumentException(
                            "Unsupported type: " + filter.type() + " for operator: " + filter.operator()
                        );
                    };
                yield criteriaBuilder.isTrue(
                    criteriaBuilder.function("sql", Boolean.class, criteriaBuilder.literal(condition))
                );
            }
            case LESS_THAN_OR_EQUALS -> {
                String condition =
                    switch (filter.type()) {
                        case INTEGER -> "value @@ '$.value <= " + filter.value() + "'";
                        case BIGDECIMAL -> "value @@ '$.value::NUMERIC <= " + filter.value() + "'";
                        case STRING -> "value @@ '$.value <= \"" + filter.value() + "\"'";
                        case DATETIME -> "value @@ '$.value::TIMESTAMPTZ <= \"" + filter.value() + "\"'";
                        case DATE -> "value @@ '$.value::DATE <= \"" + filter.value() + "\"'";
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
