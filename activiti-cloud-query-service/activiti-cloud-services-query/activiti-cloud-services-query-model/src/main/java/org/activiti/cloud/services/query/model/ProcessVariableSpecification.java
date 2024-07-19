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

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public class ProcessVariableSpecification {

    public static Specification<ProcessVariableInstance> withDynamicConditions(
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
