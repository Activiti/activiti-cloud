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

    /**
     * Enables persistence of process variable change history in the
     * {@code PROCESS_VARIABLE_HISTORY} table (flag {@code false} by default).
     *
     * <p>When disabled, {@code VARIABLE_CREATED}, {@code VARIABLE_UPDATED} and
     * {@code VARIABLE_DELETED} events are still applied to the live
     * {@code ProcessVariableEntity} state but no history record is written.
     *
     * <p>The default {@link org.activiti.cloud.common.feature.EnvironmentFeatureToggle}
     * reads property {@code activiti.features.query.process-variable-history.enabled}.
     */
    public static final String PROCESS_VARIABLE_HISTORY = "query.process-variable-history";
}
