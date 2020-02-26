/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.Resources;

public interface TaskVariablesRuntimeAdminService {

    @RequestLine("POST /admin/v1/tasks/{taskId}/variables")
    @Headers("Content-Type: application/json")
    void createTaskVariable(@Param("taskId") String taskId, CreateTaskVariablePayload payload);

    @RequestLine("PUT /admin/v1/tasks/{taskId}/variables/{variableName}")
    @Headers("Content-Type: application/json")
    void updateTaskVariable(@Param("taskId") String taskId,
                            @Param("variableName") String variableName,
                            UpdateTaskVariablePayload payload);

    @RequestLine("GET admin/v1/tasks/{taskId}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    Resources<CloudVariableInstance> getVariables(@Param("taskId") String taskId);

}
