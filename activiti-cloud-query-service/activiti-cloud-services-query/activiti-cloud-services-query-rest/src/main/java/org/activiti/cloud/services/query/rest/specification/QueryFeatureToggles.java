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

/**
 * Centralized definitions of {@link org.activiti.cloud.common.feature.FeatureToggle} flag names
 * (without the {@code activiti.features.} prefix or {@code .enabled} suffix) used by the
 * query-service REST module.
 */
public final class QueryFeatureToggles {

    /**
     * Switches the {@link ProcessInstanceSpecification} and {@link TaskSpecification} between the
     * legacy join-based queries (flag {@code false}, the default) and the {@code EXISTS}
     * subquery-based queries (flag {@code true}).
     *
     * @deprecated Since the default was changed to use EXISTS subqueries, this flag is no longer
     *             evaluated. Use {@link #FEATURE_LEGACY_JOINS} (set to {@code true}) to
     *             re-enable the legacy join-based behavior if needed.
     */
    @Deprecated(forRemoval = true)
    public static final String FEATURE_EXISTS_SUBQUERIES = "query.specifications.exists-subqueries";

    /**
     * When set to {@code true}, reverts the {@link ProcessInstanceSpecification} and
     * {@link TaskSpecification} to the legacy join-based queries that use {@code LEFT JOIN}s
     * and {@code SELECT DISTINCT}. The default ({@code false}) uses the more efficient
     * {@code EXISTS}-subquery variant that avoids row duplication and produces better
     * execution plans.
     *
     * <p>Property: {@code activiti.features.query.specifications.legacy-joins.enabled}
     */
    public static final String FEATURE_LEGACY_JOINS = "query.specifications.legacy-joins";
}
