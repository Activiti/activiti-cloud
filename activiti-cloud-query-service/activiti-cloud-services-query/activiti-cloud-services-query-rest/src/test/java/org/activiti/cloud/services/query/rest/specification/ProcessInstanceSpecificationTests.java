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

import jakarta.persistence.criteria.JoinType;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity_;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.junit.jupiter.api.Test;

/**
 * Verifies that toggling the {@link QueryFeatureToggles#FEATURE_LEGACY_JOINS} flag changes
 * the way {@link ProcessInstanceSpecification} builds the user-restriction predicate: when the
 * flag is OFF (default) correlated EXISTS subqueries are produced and {@code DISTINCT} is
 * skipped; when the flag is ON the legacy join-based code path is used and the outer query is
 * forced to {@code SELECT DISTINCT}.
 */
class ProcessInstanceSpecificationTests extends SpecificationFeatureToggleTestSupport {

    @Test
    void shouldSkipDistinctAndCreateSubqueries_byDefault() {
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

    @Test
    void shouldAddDistinctAndJoinTasks_whenLegacyJoinsToggleIsOn() {
        enableLegacyJoinsToggle();
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
}
