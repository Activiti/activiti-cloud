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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration that registers a {@link VariableRequestSizeLimitFilter} for process
 * and task variable endpoints, and a {@link VariableValueSizeValidationAdvice} that validates
 * per-variable value sizes after deserialization.
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code activiti.cloud.services.variables.max-request-size-bytes} — maximum total
 *       request body size (default: 5 MB)</li>
 *   <li>{@code activiti.cloud.services.variables.max-variable-value-length} — maximum combined
 *       size of non-whitelisted variable values (default: 8000 bytes)</li>
 *   <li>{@code activiti.cloud.services.variables.whitelisted-variables} — comma-separated list
 *       of variable names exempt from the per-variable size limit</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class VariableRequestSizeLimitAutoConfiguration {

    /**
     * Default maximum request body size: 5 MB.
     */
    private static final long DEFAULT_MAX_REQUEST_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * Default maximum combined size of non-whitelisted variable values: 8000 bytes.
     */
    private static final long DEFAULT_MAX_VARIABLE_VALUE_LENGTH = 8000;

    @Bean
    public FilterRegistrationBean<VariableRequestSizeLimitFilter> variableRequestSizeLimitFilterRegistration(
        @Value(
            "${activiti.cloud.services.variables.max-request-size-bytes:" + DEFAULT_MAX_REQUEST_SIZE_BYTES + "}"
        ) long maxRequestSizeBytes
    ) {
        FilterRegistrationBean<VariableRequestSizeLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new VariableRequestSizeLimitFilter(maxRequestSizeBytes));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("variableRequestSizeLimitFilter");
        return registration;
    }

    @Bean
    public VariableValueSizeValidator variableValueSizeValidator(
        @Value(
            "${activiti.cloud.services.variables.max-variable-value-length:" + DEFAULT_MAX_VARIABLE_VALUE_LENGTH + "}"
        ) long maxVariableValueLength,
        Environment environment
    ) {
        Set<String> whitelistedVariables = Binder.get(environment)
            .bind("activiti.cloud.services.variables.whitelisted-variables", Set.class)
            .orElse(Collections.emptySet());

        return new VariableValueSizeValidator(maxVariableValueLength, whitelistedVariables);
    }

    @Bean
    public VariableValueSizeValidationAdvice variableValueSizeValidationAdvice(VariableValueSizeValidator validator) {
        return new VariableValueSizeValidationAdvice(validator);
    }
}
