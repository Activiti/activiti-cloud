package org.activiti.cloud.services.query.model;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public class ProcessVariableSpecification {

    public static Specification<ProcessVariableEntity> withDynamicConditions(
        Set<String> processInstanceIds,
        Set<ProcessVariableKey> processVariableKeys
    ) {
        return (root, query, criteriaBuilder) -> {
            Optional<Predicate> keyFilter = processVariableKeys
                .stream()
                .map(processVariableKey -> {
                    Expression<String> processVariableKeyExpression = root.get("processDefinitionKey");
                    Expression<String> processVariableValueExpression = root.get("name");
                    return criteriaBuilder.and(
                        criteriaBuilder.equal(processVariableKeyExpression, processVariableKey.processDefinitionKey()),
                        criteriaBuilder.equal(processVariableValueExpression, processVariableKey.variableName())
                    );
                })
                .reduce(criteriaBuilder::or);

            return keyFilter.isPresent()
                ? criteriaBuilder.and(root.get("processInstanceId").in(processInstanceIds), keyFilter.get())
                : root.get("processInstanceId").in(processInstanceIds);
        };
    }
}
