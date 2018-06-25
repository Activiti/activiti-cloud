/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

/**
 * Config properties
 */
@Configuration
@Import(BaseTestsConfigurationProperties.class)
@Primary
@PropertySource("classpath:config-${profile:env}.properties")
public class RuntimeTestsConfigurationProperties {

    @Value("${audit.event.url}")
    private String auditEventUrl;

    @Value("${runtime.bundle.url}")
    private String runtimeBundleUrl;

    @Value("${runtime.bundle.another.url}")
    private String runtimeBundleAnotherUrl;

    @Value("${query.url}")
    private String queryUrl;

    public String getAuditEventUrl() {
        return auditEventUrl;
    }

    public String getRuntimeBundleUrl() {
        return runtimeBundleUrl;
    }

    public String getRuntimeBundleAnotherUrl() {
        return runtimeBundleAnotherUrl;
    }

    public String getQueryUrl() {
        return queryUrl;
    }
}
