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
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.Set;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.dialect.JsonValueFunctions;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.springframework.data.jpa.domain.Specification;

public abstract class SpecificationSupport<T> implements Specification<T> {

    protected void addLikeFilters(
        Collection<Predicate> predicates,
        Set<String> valuesToFilter,
        Root<T> root,
        CriteriaBuilder criteriaBuilder,
        SingularAttribute<T, String> attribute
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

    protected Predicate[] getProcessVariableValueFilters(
        Root<ProcessVariableEntity> root,
        Collection<VariableFilter> filters,
        CriteriaBuilder criteriaBuilder
    ) {
        return filters
            .stream()
            .map(filter ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(
                        root.get(ProcessVariableEntity_.processDefinitionKey),
                        filter.processDefinitionKey()
                    ),
                    criteriaBuilder.equal(root.get(ProcessVariableEntity_.name), filter.name()),
                    getVariableValueCondition(root.get(ProcessVariableEntity_.value), filter, criteriaBuilder)
                )
            )
            .toArray(Predicate[]::new);
    }

    protected Predicate getVariableValueCondition(
        Path<?> valueColumnPath,
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder
    ) {
        return criteriaBuilder.isTrue(
            switch (filter.operator()) {
                case EQUALS -> switch (filter.type()) {
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
                };
                case LIKE -> criteriaBuilder.function(
                    JsonValueFunctions.LIKE_CASE_INSENSITIVE,
                    Boolean.class,
                    valueColumnPath,
                    criteriaBuilder.literal(filter.value())
                );
                case GREATER_THAN -> switch (filter.type()) {
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
                };
                case GREATER_THAN_OR_EQUAL -> switch (filter.type()) {
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
                };
                case LESS_THAN -> switch (filter.type()) {
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
                };
                case LESS_THAN_OR_EQUAL -> switch (filter.type()) {
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
                };
            }
        );
    }
}
