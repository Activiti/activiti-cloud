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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.SetAttribute;
import java.util.List;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity_;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/**
 * Verifies that toggling the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} flag changes
 * the way {@link TaskSpecification} builds its predicates: when the flag is OFF (default) the
 * legacy join-based code paths are used and the outer query is forced to {@code SELECT DISTINCT};
 * when the flag is ON correlated EXISTS subqueries are produced instead and {@code DISTINCT} is
 * skipped.
 */
class TaskSpecificationTests extends SpecificationFeatureToggleTestSupport {

    @Nested
    class UserRestriction {

        @Test
        void shouldAddDistinctAndJoinCandidateUsersAndGroups_whenExistsSubqueriesToggleIsOff() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.restricted(request, USER, List.of("group1"));
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query()).distinct(true);
            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateUsers, JoinType.LEFT);
            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateGroups, JoinType.LEFT);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }

        @Test
        void shouldSkipDistinctAndCreateSubqueries_whenExistsSubqueriesToggleIsOn() {
            enableExistsSubqueriesToggle();
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
    }

    @Nested
    class GroupRestriction {

        /**
         * The generated static metamodel attributes ({@code TaskEntity_.taskCandidateUsers} and friends) are
         * {@code null} outside a JPA container, so verifying a join on a <em>specific</em> attribute is
         * really just {@code eq(null)} and cannot distinguish the candidate-user join from the
         * candidate-group one. These tests therefore assert on the number of set-attribute LEFT joins:
         * the per-user restriction makes two (candidate users + candidate groups), the group-only
         * restriction makes one (candidate groups), because its candidate-user check is an
         * {@code isEmpty()} on the path rather than a join.
         */
        private static void verifySetAttributeLeftJoins(CriteriaContext<TaskEntity> ctx, int expected) {
            verify(ctx.root(), times(expected)).join(
                ArgumentMatchers.<SetAttribute<TaskEntity, TaskCandidateGroupEntity>>any(),
                eq(JoinType.LEFT)
            );
        }

        @Test
        void shouldAddDistinctAndJoinCandidateGroupsOnly_whenExistsSubqueriesToggleIsOff() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.forGroups(request, List.of("group1"));
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query()).distinct(true);
            verifySetAttributeLeftJoins(ctx, 1);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }

        @Test
        void shouldJoinOneFewerTimeThanThePerUserRestriction_whenExistsSubqueriesToggleIsOff() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            CriteriaContext<TaskEntity> groupCtx = newCriteriaContext();
            CriteriaContext<TaskEntity> userCtx = newCriteriaContext();

            TaskSpecification.forGroups(request, List.of("group1")).toPredicate(
                groupCtx.root(),
                groupCtx.query(),
                groupCtx.cb()
            );
            TaskSpecification.restricted(request, USER, List.of("group1")).toPredicate(
                userCtx.root(),
                userCtx.query(),
                userCtx.cb()
            );

            verifySetAttributeLeftJoins(groupCtx, 1);
            verifySetAttributeLeftJoins(userCtx, 2);
        }

        @Test
        void shouldSkipDistinctAndCreateSubqueries_whenExistsSubqueriesToggleIsOn() {
            enableExistsSubqueriesToggle();
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.forGroups(request, List.of("group1"));
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), never()).distinct(true);
            // group-restriction creates 3 correlated subqueries:
            // candidate group / no-candidate-user / no-candidate-group. One fewer than the per-user
            // variant, which also needs the candidate-user EXISTS.
            verify(ctx.query(), atLeast(3)).subquery(any(Class.class));
            verifySetAttributeLeftJoins(ctx, 0);
        }

        @Test
        void shouldRejectEmptyGroups() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();

            assertThatThrownBy(() -> TaskSpecification.forGroups(request, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
            assertThatThrownBy(() -> TaskSpecification.forGroups(request, null)).isInstanceOf(
                IllegalArgumentException.class
            );
        }

        @Test
        void shouldNotRestrictAtAll_whenNeitherUserNorGroupsAreGiven() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().build();
            TaskSpecification spec = TaskSpecification.unrestricted(request);
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            // The admin tier must stay unrestricted: no restriction joins, no restriction subqueries.
            verifySetAttributeLeftJoins(ctx, 0);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }
    }

    @Nested
    class CandidateUserFilter {

        @Test
        void shouldUseJoin_whenExistsSubqueriesToggleIsOff() {
            TaskSearchRequest request = new TaskSearchRequestBuilder().withCandidateUserId(USER).build();
            TaskSpecification spec = TaskSpecification.unrestricted(request);
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.root(), atLeastOnce()).join(TaskEntity_.taskCandidateUsers);
            verify(ctx.query(), never()).subquery(any(Class.class));
        }

        @Test
        void shouldUseSubquery_whenExistsSubqueriesToggleIsOn() {
            enableExistsSubqueriesToggle();
            TaskSearchRequest request = new TaskSearchRequestBuilder().withCandidateUserId(USER).build();
            TaskSpecification spec = TaskSpecification.unrestricted(request);
            CriteriaContext<TaskEntity> ctx = newCriteriaContext();

            spec.toPredicate(ctx.root(), ctx.query(), ctx.cb());

            verify(ctx.query(), atLeastOnce()).subquery(any(Class.class));
            verify(ctx.root(), never()).join(TaskEntity_.taskCandidateUsers);
        }
    }
}
