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
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.rest.payload.ProcessVariableFilterRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

public abstract class SpecificationSupport<T, R extends ProcessVariableFilterRequest> implements Specification<T> {

    protected final R searchRequest;
    protected List<Predicate> predicates;
    protected List<VariableValueFilterCondition> filterConditions;
    private SetJoin<T, ProcessVariableEntity> pvJoin;
    protected final Map<VariableType, Class<?>> javaTypeMapping = Map.of(
        VariableType.STRING,
        String.class,
        VariableType.INTEGER,
        Integer.class,
        VariableType.BIGDECIMAL,
        BigDecimal.class,
        VariableType.DATE,
        LocalDate.class,
        VariableType.DATETIME,
        LocalDateTime.class,
        VariableType.BOOLEAN,
        Boolean.class
    );

    protected SpecificationSupport(R searchRequest) {
        this.searchRequest = searchRequest;
    }

    protected abstract SingularAttribute<T, ?> getIdAttribute();

    protected void reset() {
        predicates = new ArrayList<>();
        filterConditions = new ArrayList<>();
        pvJoin = null;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        applyProcessVariableFilters(joinProcessVariables(root), criteriaBuilder);
        if (!filterConditions.isEmpty()) {
            query.groupBy(root.get(getIdAttribute()));
            query.having(
                filterConditions
                    .stream()
                    .map(VariableValueFilterCondition::getPredicate)
                    .reduce(criteriaBuilder::and)
                    .orElse(criteriaBuilder.conjunction())
            );
        }
        if (!query.getResultType().equals(Long.class)) {
            applySorting(root, joinProcessVariables(root), query, criteriaBuilder);
        }
        if (CollectionUtils.isEmpty(query.getGroupList())) {
            query.distinct(true);
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    protected void applyProcessVariableFilters(
        Supplier<SetJoin<T, ProcessVariableEntity>> joinSupplier,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(searchRequest.processVariableFilters())) {
            SetJoin<T, ProcessVariableEntity> pvRoot = joinSupplier.get();
            filterConditions.addAll(
                searchRequest
                    .processVariableFilters()
                    .stream()
                    .map(filter ->
                        new VariableValueFilterConditionImpl<>(
                            (SetJoin<T, ? extends AbstractVariableEntity>) pvRoot,
                            Map.of(
                                pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                                filter.processDefinitionKey(),
                                pvRoot.get(ProcessVariableEntity_.name),
                                filter.name()
                            ),
                            javaTypeMapping.get(filter.type()),
                            filter,
                            criteriaBuilder
                        )
                    )
                    .toList()
            );
        }
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

    protected void applySorting(
        Root<T> root,
        Supplier<SetJoin<T, ProcessVariableEntity>> joinSupplier,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder
    ) {
        CloudRuntimeEntitySort sort = searchRequest.sort();
        if (sort != null) {
            validateSort(sort);
            Expression<?> orderByClause;
            if (sort.isProcessVariable()) {
                From<T, ProcessVariableEntity> joinRoot = joinSupplier.get();
                orderByClause =
                    new VariableSelectionExpressionImpl<>(
                        joinRoot,
                        Map.of(
                            joinRoot.get(ProcessVariableEntity_.processDefinitionKey),
                            sort.processDefinitionKey(),
                            joinRoot.get(ProcessVariableEntity_.name),
                            sort.field()
                        ),
                        javaTypeMapping.get(sort.type()),
                        criteriaBuilder
                    )
                        .getSelectionExpression();
                query.groupBy(root.get(getIdAttribute()));
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
            if (sort.processDefinitionKey() == null) {
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

    /**
     * Using a supplier to actually join process variable only if needed.
     * The instance of set join is stored in a field to avoid multiple joins.
     * @param root Specification root
     * @return Supplier of SetJoin of process variables
     */
    protected Supplier<SetJoin<T, ProcessVariableEntity>> joinProcessVariables(Root<T> root) {
        return () -> {
            if (pvJoin == null) {
                pvJoin = root.join(getProcessVariablesAttribute(), JoinType.LEFT);
            }
            return pvJoin;
        };
    }

    protected abstract SetAttribute<T, ProcessVariableEntity> getProcessVariablesAttribute();
}
