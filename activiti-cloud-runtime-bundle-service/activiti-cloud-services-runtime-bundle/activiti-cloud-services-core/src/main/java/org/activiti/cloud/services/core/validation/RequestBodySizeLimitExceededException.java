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

/**
 * Thrown when the raw request body exceeds the configured maximum size
 * ({@code activiti.cloud.variable.max-request-size}) during deserialization.
 * This is a {@link RuntimeException} so it propagates through Jackson and
 * Spring's message converter layer without being wrapped, allowing
 * {@code RuntimeBundleExceptionHandler} to map it to a clear 400 response.
 */
public class RequestBodySizeLimitExceededException extends RuntimeException {

    private final int maxRequestSize;

    public RequestBodySizeLimitExceededException(int maxRequestSize) {
        super(String.format("Request body exceeds the configured maximum size of %d bytes", maxRequestSize));
        this.maxRequestSize = maxRequestSize;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }
}
