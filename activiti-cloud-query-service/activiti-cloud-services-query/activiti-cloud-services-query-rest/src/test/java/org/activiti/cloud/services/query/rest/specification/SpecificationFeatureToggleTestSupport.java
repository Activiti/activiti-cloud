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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Answers;

/**
 * Shared scaffolding for the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} behavior
 * tests of the {@code SpecificationSupport} subclasses. Provides:
 * <ul>
 *     <li>helpers to enable / reset the feature toggle at test level;</li>
 *     <li>a factory of deep-stubbed JPA criteria mocks pre-configured to drive the
 *     {@link SpecificationSupport#toPredicate} control flow without {@code NullPointerException}s.</li>
 * </ul>
 */
abstract class SpecificationFeatureToggleTestSupport {

    protected static final String USER = "user1";

    @AfterEach
    void resetToggle() {
        FeatureToggleHolder.reset();
    }

    protected static void enableExistsSubqueriesToggle() {
        FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES::equals);
    }

    protected static <T> CriteriaContext<T> newCriteriaContext() {
        @SuppressWarnings("unchecked")
        Root<T> root = mock(Root.class, Answers.RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class, Answers.RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, Answers.RETURNS_DEEP_STUBS);
        // toPredicate needs these to drive control flow without NPEs
        lenient().when(query.getResultType()).thenAnswer(inv -> Object.class);
        lenient().when(query.getGroupList()).thenReturn(List.of());
        return new CriteriaContext<>(root, query, cb);
    }

    protected record CriteriaContext<T>(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {}
}
