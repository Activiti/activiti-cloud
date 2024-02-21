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
package org.activiti.cloud.services.rest.api;

import io.swagger.v3.oas.annotations.Parameter;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public interface TaskVariableController {
    @GetMapping(path = "/v1/tasks/{taskId}/variables", consumes = APPLICATION_JSON_VALUE)
    CollectionModel<EntityModel<CloudVariableInstance>> getVariables(
        @Parameter(description = "Enter the taskId to get variables") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(path = "/v1/tasks/{taskId}/variables", consumes = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createVariable(
        @Parameter(description = "Enter the taskId to create variable") @PathVariable(value = "taskId") String taskId,
        @RequestBody CreateTaskVariablePayload createTaskVariablePayload
    );

    @PutMapping(value = "/v1/tasks/{taskId}/variables/{variableName}", consumes = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updateVariable(
        @Parameter(description = "Enter the taskId to update variable") @PathVariable(value = "taskId") String taskId,
        @PathVariable(value = "variableName") String variableName,
        @RequestBody UpdateTaskVariablePayload updateTaskVariablePayload
    );
}
