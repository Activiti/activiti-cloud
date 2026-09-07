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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * Verifies that toggling the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} flag changes
 * the way {@link ProcessInstanceSpecification} builds the user-restriction predicate: when the
 * flag is OFF (default) the legacy join-based code path is used and the outer query is forced
 * to {@code SELECT DISTINCT}; when the flag is ON correlated EXISTS subqueries are produced
 * instead and {@code DISTINCT} is skipped.
 */
class ProcessInstanceSpecificationTests extends SpecificationFeatureToggleTestSupport {

    @Test
    void shouldAddDistinctAndJoinTasks_whenExistsSubqueriesToggleIsOff() {
        // toggle OFF (default)
        ProcessInstanceSpecification spec = ProcessInstanceSpecification.restricted(
            new ProcessInstanceSearchRequest(),
            USER
        );
        CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

        spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

        verify(ctx.query()).distinct(true);
        verify(ctx.root(), atLeastOnce()).join(ProcessInstanceEntity_.tasks, JoinType.LEFT);
        verify(ctx.query(), never()).subquery(any(Class.class));
    }

    @Test
    void shouldSkipDistinctAndCreateSubqueries_whenExistsSubqueriesToggleIsOn() {
        enableExistsSubqueriesToggle();
        ProcessInstanceSpecification spec = ProcessInstanceSpecification.restricted(
            new ProcessInstanceSearchRequest(),
            USER
        );
        CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

        spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

        verify(ctx.query(), never()).distinct(true);
        // user-restriction adds two correlated EXISTS subqueries (assignee + candidate user)
        verify(ctx.query(), atLeast(2)).subquery(any(Class.class));
        verify(ctx.root(), never()).join(eq(ProcessInstanceEntity_.tasks), any(JoinType.class));
    }

    @Nested
    class ProcessVariableFilter {

        private static final VariableFilter FILTER = new VariableFilter(
            "processDefKey",
            "varName",
            VariableType.STRING,
            "value",
            FilterOperator.EQUALS
        );

        @Test
        void shouldJoinAndGroupBy_whenExistsSubqueriesToggleIsOff() {
            ProcessInstanceSearchRequest request = new ProcessInstanceSearchRequestBuilder()
                .withProcessVariableFilters(FILTER)
                .build();
            ProcessInstanceSpecification spec = ProcessInstanceSpecification.unrestricted(request);
            CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.root(), atLeastOnce()).join(eq(ProcessInstanceEntity_.variables), eq(JoinType.LEFT));
            verify(ctx.query(), atLeastOnce()).having(any(Expression.class));
            verify(ctx.query(), never()).subquery(any(Class.class));
        }

        @Test
        void shouldUseExistsSubquery_whenExistsSubqueriesToggleIsOn() {
            enableExistsSubqueriesToggle();
            ProcessInstanceSearchRequest request = new ProcessInstanceSearchRequestBuilder()
                .withProcessVariableFilters(FILTER)
                .build();
            ProcessInstanceSpecification spec = ProcessInstanceSpecification.unrestricted(request);
            CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), atLeastOnce()).subquery(any(Class.class));
            verify(ctx.root(), never()).join(eq(ProcessInstanceEntity_.variables), eq(JoinType.LEFT));
            verify(ctx.query(), never()).having(any(Expression.class));
        }
    }

    @Nested
    class ProcessVariableSort {

        private static final CloudRuntimeEntitySort SORT = new CloudRuntimeEntitySort(
            "varName",
            Sort.Direction.ASC,
            true,
            "processDefKey",
            VariableType.STRING
        );

        @Test
        void shouldJoinAndGroupBy_whenExistsSubqueriesToggleIsOff() {
            ProcessInstanceSearchRequest request = new ProcessInstanceSearchRequestBuilder().withSort(SORT).build();
            ProcessInstanceSpecification spec = ProcessInstanceSpecification.unrestricted(request);
            CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.root(), atLeastOnce()).join(eq(ProcessInstanceEntity_.variables), eq(JoinType.LEFT));
            verify(ctx.query(), never()).subquery(any(Class.class));
        }

        @Test
        void shouldUseCorrelatedSubquery_whenExistsSubqueriesToggleIsOn() {
            enableExistsSubqueriesToggle();
            ProcessInstanceSearchRequest request = new ProcessInstanceSearchRequestBuilder().withSort(SORT).build();
            ProcessInstanceSpecification spec = ProcessInstanceSpecification.unrestricted(request);
            CriteriaContext<ProcessInstanceEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), atLeastOnce()).subquery(any(Class.class));
            verify(ctx.root(), never()).join(eq(ProcessInstanceEntity_.variables), eq(JoinType.LEFT));
        }
    }
}
