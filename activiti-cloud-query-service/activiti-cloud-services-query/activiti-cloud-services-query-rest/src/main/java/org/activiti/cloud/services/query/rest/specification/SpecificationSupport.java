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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
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
import java.util.function.Supplier;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.model.AbstractVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity_;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntityFilterRequest;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Applies the {@code processVariableFilters} of the search request.
     * <p>
     * When {@link #useExistsSubqueries()} is enabled, each filter is translated into an
     * independent correlated {@code EXISTS} subquery against the process-variable association,
     * avoiding the {@code LEFT JOIN} fan-out (one row per matching process variable) and the
     * subsequent {@code GROUP BY}/{@code HAVING} on a {@code MAX(CASE ...)} aggregate that the
     * legacy join-based approach requires. This lets the database push the outer query's
     * pagination ({@code LIMIT}/{@code OFFSET}) down instead of first materializing and grouping
     * every matching (entity, process variable) row.
     * <p>
     * When disabled (the default), the legacy behavior is preserved: all filters share a single
     * {@code LEFT JOIN} and are added to {@link #filterConditions} to be combined into a
     * {@code HAVING} clause alongside a {@code GROUP BY} on the entity id.
     */
    protected void applyProcessVariableFilters(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (CollectionUtils.isEmpty(searchRequest.processVariableFilters())) {
            return;
        }
        if (useExistsSubqueries()) {
            searchRequest
                .processVariableFilters()
                .forEach(filter ->
                    predicates.add(buildProcessVariableExistsPredicate(root, query, criteriaBuilder, filter))
                );
        } else {
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

    /**
     * Builds {@code EXISTS (SELECT pv.id FROM ... WHERE pv.processDefinitionKey = ? AND
     * pv.name = ? AND <value predicate> AND <correlation to root>)} for a single process
     * variable filter, correlated to the outer query's {@code root}.
     */
    private Predicate buildProcessVariableExistsPredicate(
        Root<T> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder,
        VariableFilter filter
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<T> correlatedRoot = subquery.correlate(root);
        SetJoin<T, ProcessVariableEntity> pvJoin = correlatedRoot.join(getProcessVariablesAttribute());
        VariableValueFilterConditionImpl<T, ProcessVariableEntity> condition = new VariableValueFilterConditionImpl<>(
            pvJoin,
            Map.of(
                pvJoin.get(ProcessVariableEntity_.processDefinitionKey),
                filter.processDefinitionKey(),
                pvJoin.get(ProcessVariableEntity_.name),
                filter.name()
            ),
            javaTypeMapping.get(filter.type()),
            filter,
            criteriaBuilder
        );
        subquery.select(pvJoin.get(ProcessVariableEntity_.id));
        subquery.where(condition.getRowPredicate());
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
                if (useExistsSubqueries()) {
                    orderByClause = buildProcessVariableSortExpression(root, query, criteriaBuilder, sort);
                } else {
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
                }
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

    /**
     * Builds a correlated scalar subquery selecting the (aggregated, to guard against
     * duplicate rows) value of the process variable being sorted on, e.g.
     * {@code (SELECT MAX(pv.value ->> 'x') FROM ... WHERE pv.processDefinitionKey = ? AND
     * pv.name = ? AND <correlation to root>)}. Unlike the legacy join-based approach, this does
     * not require joining every process variable into the outer query's {@code FROM} clause nor
     * grouping the whole result set by the entity id, since the aggregation is confined to the
     * (typically small) set of process variables matching the sort's name/definition key for a
     * single correlated row.
     */
    private Expression<?> buildProcessVariableSortExpression(
        Root<T> root,
        CriteriaQuery<?> query,
        CriteriaBuilder criteriaBuilder,
        CloudRuntimeEntitySort sort
    ) {
        Class<?> variableJavaType = javaTypeMapping.get(sort.type());
        Class<?> extractionReturnType = CustomPostgreSQLDialect.getExtractionReturnType(variableJavaType);
        @SuppressWarnings("unchecked")
        Subquery<Object> subquery = (Subquery<Object>) query.subquery(extractionReturnType);
        Root<T> correlatedRoot = subquery.correlate(root);
        SetJoin<T, ProcessVariableEntity> pvJoin = correlatedRoot.join(getProcessVariablesAttribute());
        VariableSelectionExpressionImpl<T, ProcessVariableEntity> selection = new VariableSelectionExpressionImpl<>(
            pvJoin,
            Map.of(
                pvJoin.get(ProcessVariableEntity_.processDefinitionKey),
                sort.processDefinitionKey(),
                pvJoin.get(ProcessVariableEntity_.name),
                sort.field()
            ),
            variableJavaType,
            criteriaBuilder
        );
        subquery.where(selection.getSelectionPredicate());
        subquery.select((Expression) criteriaBuilder.greatest((Expression) selection.getExtractedValue()));
        return (Expression<?>) subquery;
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
