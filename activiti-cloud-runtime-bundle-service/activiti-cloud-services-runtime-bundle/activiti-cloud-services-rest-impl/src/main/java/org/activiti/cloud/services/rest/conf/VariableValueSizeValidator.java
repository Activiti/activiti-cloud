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
