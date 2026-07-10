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
package org.activiti.cloud.services.query;

/**
 * Centralized definitions of {@link org.activiti.cloud.common.feature.FeatureToggle} flag names
 * (without the {@code activiti.features.} prefix or {@code .enabled} suffix) used across the
 * query service modules.
 */
public final class QueryFeatureToggles {

    private QueryFeatureToggles() {}

    /**
     * Switches the {@link org.activiti.cloud.services.query.rest.specification.ProcessInstanceSpecification}
     * and {@link org.activiti.cloud.services.query.rest.specification.TaskSpecification} between
     * the legacy join-based queries (flag {@code false}, the default) and the {@code EXISTS}
     * subquery-based queries (flag {@code true}).
     */
    public static final String FEATURE_EXISTS_SUBQUERIES = "query.specifications.exists-subqueries";
}
