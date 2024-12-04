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
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.exception.IllegalFilterException;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class SpecificationSupport2<T> implements Specification<T> {

    protected List<Predicate> predicates;
    protected List<Predicate> havingClauses;
    protected Root<ProcessVariableEntity> processVariableRoot;

    protected Root<ProcessVariableEntity> getProcessVariableRoot(CriteriaQuery<?> query) {
        if (processVariableRoot == null) {
            processVariableRoot = query.from(ProcessVariableEntity.class);
        }
        return processVariableRoot;
    }

    protected void reset() {
        predicates = new ArrayList<>();
        havingClauses = new ArrayList<>();
        processVariableRoot = null;
    }

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

    protected Predicate getHavingClause(
        Root<ProcessVariableEntity> root,
        Collection<VariableFilter> filters,
        CriteriaBuilder criteriaBuilder
    ) {
        return filters
            .stream()
            .map(filter ->
                criteriaBuilder.greaterThan(
                    criteriaBuilder.count(
                        criteriaBuilder
                            .selectCase()
                            .when(
                                criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                        root.get(ProcessVariableEntity_.processDefinitionKey),
                                        filter.processDefinitionKey()
                                    ),
                                    criteriaBuilder.equal(root.get(ProcessVariableEntity_.name), filter.name()),
                                    getVariableValueCondition(
                                        root.get(ProcessVariableEntity_.value),
                                        filter,
                                        criteriaBuilder
                                    )
                                ),
                                criteriaBuilder.literal(1)
                            )
                            .otherwise(criteriaBuilder.nullLiteral(Long.class))
                    ),
                    0L
                )
            )
            .reduce(criteriaBuilder::and)
            .orElse(criteriaBuilder.disjunction());
    }

    protected Predicate getVariableValueCondition(
        Path<?> valueColumnPath,
        VariableFilter filter,
        CriteriaBuilder criteriaBuilder
    ) {
        try {
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
        } catch (IllegalFilterException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    protected void applySorting(
        Root<T> root,
        Path<String> processInstanceId,
        CloudRuntimeEntitySort sort,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        if (sort != null) {
            validateSort(sort);
            Expression<Object> orderByClause;
            if (sort.isProcessVariable()) {
                Root<ProcessVariableEntity> pvRoot = getProcessVariableRoot(query);
                Predicate implicitJoinCondition = criteriaBuilder.equal(
                    processInstanceId,
                    pvRoot.get(ProcessVariableEntity_.processInstanceId)
                );
                predicates.add(implicitJoinCondition);
                Expression<?> extractedValue = criteriaBuilder.function(
                    CustomPostgreSQLDialect.getExtractionFunction(sort.type()),
                    Object.class,
                    pvRoot.get(ProcessVariableEntity_.value)
                );
                orderByClause =
                    criteriaBuilder
                        .selectCase()
                        .when(
                            criteriaBuilder.and(
                                pvRoot
                                    .get(ProcessVariableEntity_.processDefinitionKey)
                                    .in(sort.processDefinitionKeys()),
                                criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), sort.field())
                            ),
                            extractedValue
                        );
            } else {
                orderByClause = root.get(sort.field());
            }
            query.orderBy(
                sort.direction().isAscending()
                    ? criteriaBuilder.asc(orderByClause)
                    : criteriaBuilder.desc(orderByClause)
            );
        }
    }

    protected void validateSort(CloudRuntimeEntitySort sort) {
        if (sort.isProcessVariable()) {
            if (sort.processDefinitionKeys() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Process definition key is required when sorting by process variable"
                );
            }
            if (sort.type() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Variable type is required when sorting by process variable"
                );
            }
        }
    }
}
