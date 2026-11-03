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

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Validates that the combined serialized size of non-whitelisted variable values
 * does not exceed a configurable limit.
 * <p>
 * Whitelisted variable names (e.g. {@code batchState}) are exempt from the limit
 * and may be as large as the overall request body size allows.
 */
public class VariableValueSizeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableValueSizeValidator.class);

    private final long maxNonWhitelistedBytes;
    private final Set<String> whitelistedVariableNames;
    private final ObjectMapper objectMapper;

    public VariableValueSizeValidator(
        long maxNonWhitelistedBytes,
        Set<String> whitelistedVariableNames,
        ObjectMapper objectMapper
    ) {
        this.maxNonWhitelistedBytes = maxNonWhitelistedBytes;
        this.whitelistedVariableNames = whitelistedVariableNames;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a map of variables (e.g. from {@code SetProcessVariablesPayload}
     * or {@code StartProcessPayload}).
     *
     * @param variables the variable name-to-value map
     * @throws VariableValueTooLargeException if the combined size of non-whitelisted
     *         variable values exceeds the limit
     */
    public void validate(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }

        long nonWhitelistedSize = 0;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (whitelistedVariableNames.contains(entry.getKey())) {
                continue;
            }
            nonWhitelistedSize += serializedSize(entry.getValue());
        }

        if (nonWhitelistedSize > maxNonWhitelistedBytes) {
            LOGGER.warn(
                "Non-whitelisted variable data of {} bytes exceeds the maximum allowed size of {} bytes",
                nonWhitelistedSize,
                maxNonWhitelistedBytes
            );
            throw new VariableValueTooLargeException(nonWhitelistedSize, maxNonWhitelistedBytes);
        }
    }

    /**
     * Validates a single variable (e.g. from {@code CreateTaskVariablePayload}
     * or {@code UpdateTaskVariablePayload}).
     *
     * @param name the variable name
     * @param value the variable value
     * @throws VariableValueTooLargeException if the variable is not whitelisted
     *         and its value exceeds the limit
     */
    public void validateSingle(String name, Object value) {
        if (whitelistedVariableNames.contains(name)) {
            return;
        }

        long size = serializedSize(value);

        if (size > maxNonWhitelistedBytes) {
            LOGGER.warn(
                "Variable '{}' value of {} bytes exceeds the maximum allowed size of {} bytes",
                name,
                size,
                maxNonWhitelistedBytes
            );
            throw new VariableValueTooLargeException(size, maxNonWhitelistedBytes);
        }
    }

    /**
     * Returns the size in UTF-8 bytes of the JSON-serialized value, using the application's
     * configured {@link ObjectMapper}. This ensures maps, lists and multi-byte characters
     * are measured consistently with how they appear on the wire.
     */
    private long serializedSize(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return objectMapper.writeValueAsBytes(value).length;
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize variable value for size check, falling back to string length", e);
            return String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
    }
}
