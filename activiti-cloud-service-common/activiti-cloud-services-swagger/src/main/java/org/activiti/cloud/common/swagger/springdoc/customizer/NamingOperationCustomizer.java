/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

package org.activiti.cloud.common.swagger.springdoc.customizer;

import io.swagger.v3.oas.models.Operation;
import org.springframework.web.method.HandlerMethod;

import java.util.Optional;

public class NamingOperationCustomizer implements DefaultOperationCustomizer {

    private static final String DEFAULT_SPRINGDOC_PATTERN_REGEX = "(_[0-9])*$";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Optional.ofNullable(operation.getOperationId()).ifPresent(operationId ->
            operation.setOperationId(operationId.replaceAll(DEFAULT_SPRINGDOC_PATTERN_REGEX, "")));
        Optional.ofNullable(operation.getSummary()).ifPresent(summary ->
            operation.setSummary(summary.replaceAll(DEFAULT_SPRINGDOC_PATTERN_REGEX, "")));
        return operation;
    }
}
