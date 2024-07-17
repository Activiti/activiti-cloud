package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.activiti.cloud.services.query.model.ProcessVariablesPivotEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public class TaskSpecification2 implements Specification<TaskEntity> {

    private final TaskSearchRequest taskSearchRequest;

    public TaskSpecification2(TaskSearchRequest taskSearchRequest) {
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
        {
            if (CollectionUtils.isEmpty(taskSearchRequest.processVariableValueFilters())) {
                return criteriaBuilder.conjunction();
            }
            Root<ProcessVariablesPivotEntity> pvRoot = query.from(ProcessVariablesPivotEntity.class);
            Predicate joinCondition = criteriaBuilder.equal(
                root.get("processInstanceId"),
                pvRoot.get("processInstanceId")
            );

            Predicate[] variableValueFilters = taskSearchRequest
                .processVariableValueFilters()
                .stream()
                .map(filter -> {
                    Expression<Boolean> function = criteriaBuilder.function(
                        "sql",
                        Boolean.class,
                        criteriaBuilder.literal(
                            "process_variables @> '{\"" +
                            filter.processDefinitionKey() +
                            "/" +
                            filter.name() +
                            "\": \"" +
                            filter.value() +
                            "\"}'"
                        )
                    );
                    return criteriaBuilder.isTrue(function);
                })
                .toArray(Predicate[]::new);

            return criteriaBuilder.and(joinCondition, criteriaBuilder.or(variableValueFilters));
        }
    }
}
