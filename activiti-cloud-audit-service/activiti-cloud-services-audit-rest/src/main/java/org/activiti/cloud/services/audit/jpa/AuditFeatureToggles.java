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
package org.activiti.cloud.services.audit.jpa;

/**
 * Centralized definitions of {@link org.activiti.cloud.common.feature.FeatureToggle} flag names
 * (without the {@code activiti.features.} prefix or {@code .enabled} suffix) used across the
 * audit service modules.
 */
public final class AuditFeatureToggles {

    private AuditFeatureToggles() {}

    /**
     * Enables the cancellable async audit events deletion endpoints with progress tracking
     * (flag {@code false} by default).
     *
     * <p>When disabled, the deletion, cancellation and status endpoints in
     * {@link org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController}
     * return {@code 404 Not Found}.
     *
     * <p>The default {@link org.activiti.cloud.common.feature.EnvironmentFeatureToggle}
     * reads property {@code activiti.features.audit.cancellable-delete.enabled}.
     */
    public static final String AUDIT_CANCELLABLE_DELETE = "audit.cancellable-delete";
}
