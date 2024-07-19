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
package org.activiti.cloud.services.query.model;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

    public static Specification<TaskEntity> withDynamicConditions(
        Set<ProcessVariableValueFilter> processVariableValueFilters
    ) {
        return (root, query, criteriaBuilder) -> {
            if (processVariableValueFilters.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Root<ProcessVariablesPivotEntity> pvRoot = query.from(ProcessVariablesPivotEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get("processInstanceId"),
                pvRoot.get("processInstanceId")
            );

            Predicate[] variableValueFilters = processVariableValueFilters
                .stream()
                .map(filter -> {
                    Predicate procDefKeyPredicate = criteriaBuilder.equal(
                        pvRoot.get("processDefinitionKey"),
                        filter.processDefinitionKey()
                    );
                    Expression<Boolean> valueExpression = criteriaBuilder.function(
                        "sql",
                        Boolean.class,
                        criteriaBuilder.literal(
                            "process_variables @@ '$." + filter.name() + ".value == \"" + filter.value() + "\"'"
                        )
                    );
                    return criteriaBuilder.and(procDefKeyPredicate, valueExpression);
                })
                .toArray(Predicate[]::new);

            return criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters));
        };
    }

    private static Predicate getVariableNameCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("name"), filter.name());
    }

    private static Predicate getProcessDefinitionCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> pvRoot
    ) {
        return criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), filter.processDefinitionKey());
    }

    private static Predicate getVariableValueCondition(
        ProcessVariableValueFilter filter,
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> root
    ) {
        return switch (filter.filterType()) {
            case EQUALS -> criteriaBuilder.equal(
                extractValueAsString(criteriaBuilder, root),
                criteriaBuilder.literal(filter.value())
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
        Root<ProcessVariableInstance> root
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
        Root<ProcessVariableInstance> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(Long.class);
    }

    private static Expression<LocalDate> extractValueAsDate(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(LocalDate.class);
    }

    private static Expression<LocalDateTime> extractValueAsDateTime(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(LocalDateTime.class);
    }

    private static Expression<BigDecimal> extractValueAsNumeric(
        CriteriaBuilder criteriaBuilder,
        Root<ProcessVariableInstance> root
    ) {
        return extractValueAsString(criteriaBuilder, root).as(BigDecimal.class);
    }
}
