package org.activiti.cloud.services.query.model;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

    public static Specification<TaskEntity> withDynamicConditions(TaskSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            Root<ProcessVariableEntity> pvRoot = query.from(ProcessVariableEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get("processInstanceId"),
                pvRoot.get("processInstanceId")
            );

            Expression<String> extractedValue = criteriaBuilder.function(
                "jsonb_extract_path_text",
                String.class,
                pvRoot.get("value"),
                criteriaBuilder.literal("value")
            );

            Predicate[] variableValueFilters = criteria
                .conditions()
                .stream()
                .map(filter ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), filter.processDefinitionKey()),
                        criteriaBuilder.equal(pvRoot.get("name"), filter.variableName()),
                        getValueCriteria(criteriaBuilder, extractedValue, filter)
                    )
                )
                .toArray(Predicate[]::new);

            Predicate[] fetchFilters = criteria
                .processVariableFetchKeys()
                .stream()
                .map(key ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(pvRoot.get("processDefinitionKey"), key.getProcessDefinitionKey()),
                        criteriaBuilder.equal(pvRoot.get("name"), key.getVariableName())
                    )
                )
                .toArray(Predicate[]::new);

            Predicate havingClause = criteriaBuilder.and(
                Arrays
                    .stream(variableValueFilters)
                    .map(variableFilterPredicate ->
                        criteriaBuilder.gt(
                            criteriaBuilder.countDistinct(
                                criteriaBuilder
                                    .selectCase()
                                    .when(variableFilterPredicate, pvRoot.get("id"))
                                    .otherwise(criteriaBuilder.nullLiteral(Long.class))
                            ),
                            0L
                        )
                    )
                    .toArray(Predicate[]::new)
            );

            query.groupBy(root.get("id"));
            query.having(havingClause);

            return criteriaBuilder.and(joinCondition, criteriaBuilder.or(fetchFilters));
        };
    }

    private static Predicate getValueCriteria(
        CriteriaBuilder criteriaBuilder,
        Expression<String> valueExpression,
        ProcessVariableValueFilter filter
    ) {
        return switch (filter.filterType()) {
            case EQUALS -> criteriaBuilder.equal(valueExpression, filter.value());
            case CONTAINS -> criteriaBuilder.like(valueExpression, "%" + filter.value() + "%");
            case RANGE -> {
                String[] range = filter.value().toString().split(",");
                yield criteriaBuilder.between(valueExpression, range[0], range[1]);
            }
        };
    }
}
