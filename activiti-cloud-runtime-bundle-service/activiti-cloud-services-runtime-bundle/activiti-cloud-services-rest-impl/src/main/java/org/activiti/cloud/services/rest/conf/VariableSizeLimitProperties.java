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

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Size limits applied to process and task variable requests.
 */
@ConfigurationProperties("activiti.cloud.services.variables")
public class VariableSizeLimitProperties {

    /**
     * Maximum total request body size in bytes, enforced by {@link VariableRequestSizeLimitFilter}.
     */
    private long maxRequestSizeBytes = 5L * 1024 * 1024;

    /**
     * Maximum combined size of non-whitelisted variable values, enforced by
     * {@link VariableValueSizeValidationAdvice}.
     */
    private long maxVariableValueLength = 8000;

    /**
     * Variable names exempt from {@link #maxVariableValueLength}. Accepts a comma-separated list.
     */
    private Set<String> whitelistedVariables = new HashSet<>();

    public long getMaxRequestSizeBytes() {
        return maxRequestSizeBytes;
    }

    public void setMaxRequestSizeBytes(long maxRequestSizeBytes) {
        this.maxRequestSizeBytes = maxRequestSizeBytes;
    }

    public long getMaxVariableValueLength() {
        return maxVariableValueLength;
    }

    public void setMaxVariableValueLength(long maxVariableValueLength) {
        this.maxVariableValueLength = maxVariableValueLength;
    }

    public Set<String> getWhitelistedVariables() {
        return whitelistedVariables;
    }

    public void setWhitelistedVariables(Set<String> whitelistedVariables) {
        this.whitelistedVariables = whitelistedVariables;
    }
}
