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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the combined size of non-whitelisted variable values
 * exceeds the allowed limit.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VariableValueTooLargeException extends RuntimeException {

    public VariableValueTooLargeException(long totalSize, long maxAllowed) {
        super(
            "Non-whitelisted variable data of " +
                totalSize +
                " bytes exceeds the maximum allowed size of " +
                maxAllowed +
                " bytes"
        );
    }
}
