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
package org.activiti.cloud.services.rest.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers a {@link VariableRequestSizeLimitFilter} for process
 * and task variable endpoints.
 * <p>
 * The maximum request body size can be configured via the property
 * {@code activiti.cloud.services.variables.max-request-size-bytes} (default: 5 MB).
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class VariableRequestSizeLimitAutoConfiguration {

    /**
     * Default maximum request body size: 5 MB.
     */
    private static final long DEFAULT_MAX_REQUEST_SIZE_BYTES = 5 * 1024 * 1024;

    @Bean
    public FilterRegistrationBean<VariableRequestSizeLimitFilter> variableRequestSizeLimitFilterRegistration(
        @Value(
            "${activiti.cloud.services.variables.max-request-size-bytes:" + DEFAULT_MAX_REQUEST_SIZE_BYTES + "}"
        ) long maxRequestSizeBytes
    ) {
        FilterRegistrationBean<VariableRequestSizeLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new VariableRequestSizeLimitFilter(maxRequestSizeBytes));
        registration.addUrlPatterns(
            "/v1/process-instances/*/variables",
            "/v1/process-instances/*/variables/*",
            "/admin/v1/process-instances/*/variables",
            "/admin/v1/process-instances/*/variables/*",
            "/v1/tasks/*/variables",
            "/v1/tasks/*/variables/*",
            "/admin/v1/tasks/*/variables",
            "/admin/v1/tasks/*/variables/*"
        );
        registration.setOrder(1);
        registration.setName("variableRequestSizeLimitFilter");
        return registration;
    }
}
