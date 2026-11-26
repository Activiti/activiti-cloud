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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Subquery;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.junit.jupiter.api.Test;

/**
 * Verifies that toggling the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} flag changes
 * the way {@link ProcessInstanceSpecification} builds the user-restriction predicate: when the
 * flag is OFF (default) the legacy join-based code path is used and the outer query is forced
 * to {@code SELECT DISTINCT}; when the flag is ON the visible process instance ids are selected
 * through a {@code UNION} of uncorrelated subqueries and {@code DISTINCT} is skipped.
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
        Path<String> idPath = ctx.root().get(ProcessInstanceEntity_.id);

        verify(ctx.query(), never()).distinct(true);
        // user-restriction unions three uncorrelated subqueries (initiator + assignee + candidate user)
        verify(ctx.query(), times(3)).subquery(String.class);
        verify((NodeBuilder) ctx.cb()).union(eq(false), any(Subquery.class), any(Subquery.class), any(Subquery.class));
        verify((NodeBuilder) ctx.cb()).in(eq(idPath), any(SqmSubQuery.class));
        verify(ctx.root(), never()).join(eq(ProcessInstanceEntity_.tasks), any(JoinType.class));
    }
}
