/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.app.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
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
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.app.filter.VariableFilter;
import org.activiti.cloud.services.query.app.filter.VariableType;
import org.activiti.cloud.services.query.app.payload.CloudRuntimeEntityFilterRequest;
import org.activiti.cloud.services.query.app.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.AbstractVariableEntity_;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public abstract class SpecificationSupport<T, R extends CloudRuntimeEntityFilterRequest> implements Specification<T> {

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

    /**
     * @return {@code true} when the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} flag is
     *         enabled in the application-wide {@link FeatureToggleHolder}, in which case
     *         subclasses must build {@code EXISTS}-subquery based predicates and skip the
     *         {@code SELECT DISTINCT} clause; {@code false} (the default) keeps the legacy
     *         join-based behavior including {@code SELECT DISTINCT}.
     */
    protected boolean useExistsSubqueries() {
        return FeatureToggleHolder.isEnabled(QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES);
    }

    protected abstract SingularAttribute<T, ?> getIdAttribute();

    protected void reset() {
        predicates = new ArrayList<>();
        filterConditions = new ArrayList<>();
        pvJoin = null;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        applyProcessVariableFilters(root, query, criteriaBuilder);
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
        // The legacy join-based predicates produce duplicate rows that need DISTINCT to be
        // collapsed. The EXISTS-subquery variant does not introduce duplicates, so DISTINCT
        // is dropped to allow more efficient query plans.
        if (!useExistsSubqueries() && CollectionUtils.isEmpty(query.getGroupList())) {
            query.distinct(true);
        }
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    protected void applyProcessVariableFilters(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (CollectionUtils.isEmpty(searchRequest.processVariableFilters())) {
            return;
        }
        if (useExistsSubqueries()) {
            searchRequest
                .processVariableFilters()
                .forEach(filter ->
                    predicates.add(
                        variableFilterExists(
                            root,
                            query,
                            criteriaBuilder,
                            getProcessVariablesAttribute(),
                            filter,
                            processVariableJoin ->
                                Map.of(
                                    processVariableJoin.get(ProcessVariableEntity_.processDefinitionKey),
                                    filter.processDefinitionKey(),
                                    processVariableJoin.get(AbstractVariableEntity_.name),
                                    filter.name()
                                )
                        )
                    )
                );
            return;
        }
        SetJoin<T, ProcessVariableEntity> pvRoot = joinProcessVariables(root).get();
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
                            pvRoot.get(AbstractVariableEntity_.name),
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

    /**
     * Builds a correlated {@code EXISTS} subquery matching a single variable filter, e.g.
     * <pre>
     * exists (select 1
     *           from task_process_variable tpv
     *           join process_variable pv on pv.id = tpv.process_variable_id
     *          where tpv.task_id = t.id
     *            and pv.process_definition_key = ? and pv.name = ?
     *            and lower(pv.value -&gt;&gt; 'value') like ?)
     * </pre>
     * Unlike the legacy {@code group by} / {@code having max(case when ... end)} form, this keeps the
     * outer query free of aggregation: the planner can then walk the {@code order by} index and stop
     * as soon as the requested page is filled instead of aggregating the whole result set first, and
     * the count query falls back to a plain {@code count(*)} rather than {@code count(*) over()}.
     * <p>
     * Both forms select the same rows because an entity holds at most one variable per
     * {@code (processDefinitionKey, name)}: with a single matching row {@code max(...)} is that row's
     * value, and when nothing matches {@code max(...)} is {@code null}, which makes every comparison
     * evaluate to unknown and discards the row exactly like a false {@code EXISTS}.
     *
     * @param variablesAttribute the variables association to correlate on
     * @param selectionFilters   builds, from the subquery join, the equality checks identifying the
     *                           variable the filter refers to
     */
    protected <V extends AbstractVariableEntity> Predicate variableFilterExists(
        Root<T> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder,
        SetAttribute<T, V> variablesAttribute,
        VariableFilter filter,
        Function<SetJoin<T, V>, Map<Path<String>, String>> selectionFilters
    ) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        SetJoin<T, V> variableJoin = subquery.correlate(root).join(variablesAttribute);
        VariableValueFilterCondition condition = new VariableValueFilterConditionImpl<>(
            variableJoin,
            selectionFilters.apply(variableJoin),
            javaTypeMapping.get(filter.type()),
            filter,
            criteriaBuilder
        );
        subquery.select(criteriaBuilder.literal(1));
        subquery.where(condition.getSubqueryPredicate());
        return criteriaBuilder.exists(subquery);
    }

    protected void applyIdFilter(Root<T> root) {
        if (!CollectionUtils.isEmpty(searchRequest.id())) {
            predicates.add(root.get(getIdAttribute()).in(searchRequest.id()));
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
                orderByClause = new VariableSelectionExpressionImpl<>(
                    joinRoot,
                    Map.of(
                        joinRoot.get(ProcessVariableEntity_.processDefinitionKey),
                        sort.processDefinitionKey(),
                        joinRoot.get(ProcessVariableEntity_.name),
                        sort.field()
                    ),
                    javaTypeMapping.get(sort.type()),
                    criteriaBuilder
                ).getSelectionExpression();
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
                throw new InvalidSortException("Process definition key is required when sorting by process variable");
            }
            if (sort.type() == null) {
                throw new InvalidSortException("Variable type is required when sorting by process variable");
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
