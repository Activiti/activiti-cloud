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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for variable size limits on process and task variable endpoints.
 * <p>
 * Registers a {@link VariableRequestSizeLimitFilter}, which enforces the total request body size,
 * and imports {@link VariableValueSizeValidationAdvice}, which enforces the combined size of
 * non-whitelisted variable values after deserialization.
 * <p>
 * Configured through {@link VariableSizeLimitProperties} ({@code activiti.cloud.services.variables.*}):
 * <ul>
 *   <li>{@code max-request-size-bytes} — maximum total request body size (default: 5 MB)</li>
 *   <li>{@code max-variable-value-length} — maximum combined size of non-whitelisted variable
 *       values (default: 8000 bytes)</li>
 *   <li>{@code whitelisted-variables} — comma-separated list of variable names exempt from the
 *       per-variable size limit</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(VariableSizeLimitProperties.class)
@Import(VariableValueSizeValidationAdvice.class)
public class VariableRequestSizeLimitAutoConfiguration {

    @Bean
    public FilterRegistrationBean<VariableRequestSizeLimitFilter> variableRequestSizeLimitFilterRegistration(
        VariableSizeLimitProperties properties
    ) {
        FilterRegistrationBean<VariableRequestSizeLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new VariableRequestSizeLimitFilter(properties.getMaxRequestSizeBytes()));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("variableRequestSizeLimitFilter");
        return registration;
    }
}
