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
import java.util.List;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies that toggling the {@link QueryFeatureToggles#FEATURE_LEGACY_JOINS} flag changes
 * the way {@link TaskSpecification} builds its predicates: when the flag is OFF (default)
 * correlated EXISTS subqueries are produced and {@code DISTINCT} is skipped; when the flag
 * is ON the legacy join-based code paths are used and the outer query is forced to
 * {@code SELECT DISTINCT}.
 */
class TaskSpecificationTests extends SpecificationFeatureToggleTestSupport {

    @Nested
    class UserRestriction {

        @Test
        void shouldSkipDistinctAndCreateSubqueries_byDefault() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.restricted(request, USER, List.of("group1"));
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), never()).distinct(true);
            // user-restriction creates 4 correlated subqueries:
            // candidate user / candidate group / no-candidate-user / no-candidate-group
            verify(ctx.query(), atLeast(4)).subquery(any(Class.class));
            verify(ctx.root(), never()).join(eq(TaskEntity_.taskCandidateUsers), any(JoinType.class));
            verify(ctx.root(), never()).join(eq(TaskEntity_.taskCandidateGroups), any(JoinType.class));
        }

        @Test
        void shouldAddDistinctAndJoinCandidateUsersAndGroups_whenLegacyJoinsToggleIsOn() {
            enableLegacyJoinsToggle();
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.restricted(request, USER, List.of("group1"));
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query()).distinct(true);
            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateUsers, JoinType.LEFT);
            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateGroups, JoinType.LEFT);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }
    }

    @Nested
    class CandidateUserFilter {

        @Test
        void shouldUseSubquery_byDefault() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().withCandidateUserId(USER).build();
            TaskSpecification spec = TaskSpecification.unrestricted(request);
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), atLeastOnce()).subquery(any(Class.class));
            verify(ctx.root(), never()).join(TaskEntity_.taskCandidateUsers);
        }

        @Test
        void shouldUseJoin_whenLegacyJoinsToggleIsOn() {
            enableLegacyJoinsToggle();
            TaskSearchRequest request = new TaskSearchRequestBuilder().withCandidateUserId(USER).build();
            TaskSpecification spec = TaskSpecification.unrestricted(request);
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateUsers);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }
    }
}
