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
package org.activiti.cloud.services.core.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "activiti.cloud.variable")
public class VariableProperties {

    public static final int DEFAULT_MAX_VALUE_SIZE = 5 * 1024 * 1024;
    private static final int REQUEST_SIZE_OVERHEAD = 1024;

    private int maxValueSize = DEFAULT_MAX_VALUE_SIZE;
    private Integer maxRequestSize;

    public int getMaxValueSize() {
        return maxValueSize;
    }

    public void setMaxValueSize(int maxValueSize) {
        this.maxValueSize = maxValueSize;
    }

    /**
     * Returns the maximum allowed request body size in bytes.
     * When explicitly configured via {@code activiti.cloud.variable.max-request-size},
     * that value is used as-is. Otherwise the limit is derived from {@code maxValueSize}:
     * disabled (0) when {@code maxValueSize} is disabled ({@code <= 0}),
     * or {@code maxValueSize + 1 KB} otherwise.
     */
    public int getMaxRequestSize() {
        if (maxRequestSize != null) {
            return maxRequestSize;
        }
        if (maxValueSize <= 0) {
            return 0;
        }
        return maxValueSize + REQUEST_SIZE_OVERHEAD;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }
}
