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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.model.VariableValue;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
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
    protected Map<String, Selection<?>> selections = new HashMap<>();
    public List<VariableValueCondition> filterConditions;

    private SetJoin<T, ProcessVariableEntity> pvJoin;

    protected SpecificationSupport(R searchRequest) {
        this.searchRequest = searchRequest;
    }

    public Map<String, Selection<?>> getSelections() {
        return selections;
    }

    protected void reset() {
        predicates = new ArrayList<>();
        filterConditions = new ArrayList<>();
        pvJoin = null;
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

    protected String getAlias(String processDefinitionKey, String processVariableName) {
        return processDefinitionKey + "_" + processVariableName;
    }

    protected VariableValueCondition getCondition(
        Predicate variablePredicate,
        CriteriaBuilder criteriaBuilder,
        Path<VariableValue> variableValuePath,
        VariableFilter filter
    ) {
        return getCondition(
            variablePredicate,
            criteriaBuilder,
            variableValuePath,
            filter.type(),
            filter.operator(),
            filter.value()
        );
    }

    protected VariableValueCondition getCondition(
        Predicate variablePredicate,
        CriteriaBuilder criteriaBuilder,
        Path<VariableValue> variableValuePath,
        VariableType variableType,
        FilterOperator filterOperator,
        String filterValue
    ) {
        try {
            return switch (variableType) {
                case STRING -> new StringVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
                case INTEGER -> new IntegerVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
                case BIGDECIMAL -> new BigDecimalVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
                case DATE -> new DateVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
                case DATETIME -> new DatetimeVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
                case BOOLEAN -> new BooleanVariableValueCondition(
                    variableValuePath,
                    variablePredicate,
                    filterOperator,
                    filterValue,
                    criteriaBuilder
                );
            };
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid filter (type: " +
                variableType +
                ", operator: " +
                filterOperator +
                ", value: " +
                filterValue +
                ")"
            );
        }
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
                SetJoin<T, ProcessVariableEntity> pvJoin = joinSupplier.get();
                orderByClause =
                    getCondition(
                        criteriaBuilder.and(
                            criteriaBuilder.equal(
                                pvJoin.get(ProcessVariableEntity_.processDefinitionKey),
                                sort.processDefinitionKey()
                            ),
                            criteriaBuilder.equal(pvJoin.get(ProcessVariableEntity_.name), sort.field())
                        ),
                        criteriaBuilder,
                        pvJoin.get(ProcessVariableEntity_.value),
                        sort.type(),
                        null,
                        null
                    )
                        .getColumnExpression();
                Selection<?> selection = selections.getOrDefault(
                    getAlias(sort.processDefinitionKey(), sort.field()),
                    orderByClause.alias(getAlias(sort.processDefinitionKey(), sort.field()))
                );
                selections.put(selection.getAlias(), selection);
                query.groupBy(root.get("id"));
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
     * @param pvAttribute Process variable attribute to join
     * @return Supplier of SetJoin of process variables
     */
    protected Supplier<SetJoin<T, ProcessVariableEntity>> joinProcessVariables(
        Root<T> root,
        SetAttribute<T, ProcessVariableEntity> pvAttribute
    ) {
        return () -> pvJoin == null ? pvJoin = root.join(pvAttribute, JoinType.LEFT) : pvJoin;
    }

    protected void applyProcessVariableFilters(
        Supplier<SetJoin<T, ProcessVariableEntity>> joinSupplier,
        CriteriaBuilder criteriaBuilder
    ) {
        if (!CollectionUtils.isEmpty(searchRequest.processVariableFilters())) {
            SetJoin<T, ProcessVariableEntity> pvRoot = joinSupplier.get();
            List<VariableValueCondition> conditions = searchRequest
                .processVariableFilters()
                .stream()
                .map(filter -> {
                    VariableValueCondition condition = getCondition(
                        criteriaBuilder.and(
                            criteriaBuilder.equal(
                                pvRoot.get(ProcessVariableEntity_.processDefinitionKey),
                                filter.processDefinitionKey()
                            ),
                            criteriaBuilder.equal(pvRoot.get(ProcessVariableEntity_.name), filter.name())
                        ),
                        criteriaBuilder,
                        pvRoot.get(ProcessVariableEntity_.value),
                        filter
                    );
                    String alias = getAlias(filter.processDefinitionKey(), filter.name());
                    selections.put(alias, condition.getColumnExpression().alias(alias));
                    return condition;
                })
                .toList();
            filterConditions.addAll(conditions);
        }
    }
}
