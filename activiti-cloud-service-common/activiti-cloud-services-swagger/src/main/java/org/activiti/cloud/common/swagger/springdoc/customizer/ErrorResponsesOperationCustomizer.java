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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

public class ErrorResponsesOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses apiResponses = operation.getResponses();
        if (!apiResponses.containsKey("401")) {
            ApiResponse unauthorizedResponse = new ApiResponse().description("Unauthorized");
            apiResponses.addApiResponse("401", unauthorizedResponse);
        }
        if (!apiResponses.containsKey("403")) {
            ApiResponse forbiddenResponse = new ApiResponse().description("Forbidden");
            apiResponses.addApiResponse("403", forbiddenResponse);
        }
        if (!apiResponses.containsKey("404")) {
            ApiResponse notFoundResponse = new ApiResponse().description("Not Found");
            apiResponses.addApiResponse("404", notFoundResponse);
        }
        return operation;
    }
}
