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
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.filter.ProcessVariableFilter;
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
        if (taskSearchRequest.rootTasksOnly()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.parentTaskId)));
        }
        if (taskSearchRequest.standAlone()) {
            predicates.add(criteriaBuilder.isNull(root.get(TaskEntity_.processInstanceId)));
        }
        if (!CollectionUtils.isEmpty(taskSearchRequest.processVariableFilters())) {
            applyProcessVariableFilters(root, query, criteriaBuilder);
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
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
        ProcessVariableFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("name"), filter.name());
    }

    private static Predicate getProcessDefinitionCondition(
        ProcessVariableFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), filter.processDefinitionKey());
    }

    private static Predicate getVariableValueCondition(
        ProcessVariableFilter filter,
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
