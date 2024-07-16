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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public class TaskSpecification1 implements Specification<TaskEntity> {

    private final TaskSearchRequest taskSearchRequest;

    public TaskSpecification1(TaskSearchRequest taskSearchRequest) {
        this.taskSearchRequest = taskSearchRequest;
    }

    @Override
    public Predicate toPredicate(Root<TaskEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.and(applyProcessVariableValueFilters(root, query, criteriaBuilder));
    }

    private Predicate applyProcessVariableValueFilters(
        Root<TaskEntity> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (CollectionUtils.isEmpty(taskSearchRequest.processVariableValueFilters())) {
            return criteriaBuilder.conjunction();
        }

        Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
        Predicate joinCondition = criteriaBuilder.equal(root.get("processInstanceId"), pvRoot.get("processInstanceId"));

        Predicate[] variableValueFilters = taskSearchRequest
            .processVariableValueFilters()
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
            .processVariableValueFilters()
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
                                pvRoot.get("id")
                            )
                            .otherwise(criteriaBuilder.nullLiteral(Long.class))
                    ),
                    criteriaBuilder.literal(0)
                )
            )
            .toArray(Predicate[]::new);

        query.groupBy(root.get("id"));
        query.having(havingClause);

        return criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters));
    }

    private static Predicate getVariableNameCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("name"), filter.name());
    }

    private static Predicate getProcessDefinitionCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), filter.processDefinitionKey());
    }

    private static Predicate getVariableValueCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return switch (filter.filterType()) {
            case EQUALS -> criteriaBuilder.isTrue(
                criteriaBuilder.function(
                    "sql",
                    Boolean.class,
                    criteriaBuilder.literal("value @@ '$.value == \"" + filter.value() + "\"'")
                )
            );
            case CONTAINS -> criteriaBuilder.like(
                extractValueAsString(criteriaBuilder, root),
                "%" + filter.value() + "%"
            );
            case GREATER_THAN -> switch (filter.type()) {
                case "integer" -> criteriaBuilder.greaterThan(
                    extractValueAsBigInteger(criteriaBuilder, root),
                    Long.parseLong(filter.value())
                );
                case "bigdecimal" -> criteriaBuilder.greaterThan(
                    extractValueAsNumeric(criteriaBuilder, root),
                    new BigDecimal(filter.value())
                );
                case "date" -> criteriaBuilder.greaterThan(
                    extractValueAsDate(criteriaBuilder, root),
                    criteriaBuilder.literal(filter.value()).as(LocalDate.class)
                );
                case "datetime" -> criteriaBuilder.greaterThan(
                    extractValueAsDateTime(criteriaBuilder, root),
                    LocalDateTime.parse(filter.value())
                );
                default -> throw new IllegalArgumentException("Unsupported type: " + filter.type());
            };
            case LESS_THAN -> switch (filter.type()) {
                case "integer" -> criteriaBuilder.lessThan(
                    extractValueAsBigInteger(criteriaBuilder, root),
                    Long.parseLong(filter.value())
                );
                case "bigdecimal" -> criteriaBuilder.lessThan(
                    extractValueAsNumeric(criteriaBuilder, root),
                    new BigDecimal(filter.value())
                );
                case "date" -> criteriaBuilder.lessThan(
                    extractValueAsDate(criteriaBuilder, root),
                    LocalDate.parse(filter.value())
                );
                case "datetime" -> criteriaBuilder.lessThan(
                    extractValueAsDateTime(criteriaBuilder, root),
                    LocalDateTime.parse(filter.value())
                );
                default -> throw new IllegalArgumentException("Unsupported type: " + filter.type());
            };
        };
    }

    private static Expression<String> extractValueAsString(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return criteriaBuilder.function(
            "jsonb_extract_path_text",
            String.class,
            root.get("value"),
            criteriaBuilder.literal("value")
        );
    }

    private static Expression<Long> extractValueAsBigInteger(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(Long.class);
    }

    private static Expression<LocalDate> extractValueAsDate(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(LocalDate.class);
    }

    private static Expression<LocalDateTime> extractValueAsDateTime(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(LocalDateTime.class);
    }

    private static Expression<BigDecimal> extractValueAsNumeric(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableEntity> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(BigDecimal.class);
    }
}
