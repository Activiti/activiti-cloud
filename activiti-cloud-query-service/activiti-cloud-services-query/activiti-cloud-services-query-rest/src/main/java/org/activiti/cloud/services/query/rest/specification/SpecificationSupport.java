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
        VariableValueCondition valueConditionStrategy =
            switch (filter.type()) {
                case STRING -> new StringVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
                case INTEGER -> new IntegerVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
                case BIGDECIMAL -> new BigDecimalVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
                case DATE -> new DateVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
                case DATETIME -> new DatetimeVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
                case BOOLEAN -> new BooleanVariableValueCondition(
                    valueColumnPath,
                    filter.operator(),
                    filter.value(),
                    criteriaBuilder
                );
            };

        return valueConditionStrategy.toPredicate();
    }
}
