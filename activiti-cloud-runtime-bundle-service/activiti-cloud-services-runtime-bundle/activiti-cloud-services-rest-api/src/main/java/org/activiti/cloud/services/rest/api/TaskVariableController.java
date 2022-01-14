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

import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TaskVariableController {

    @GetMapping(path = "/v1/tasks/{taskId}/variables", headers = "Content-type=application/json")
    CollectionModel<EntityModel<CloudVariableInstance>> getVariables(@PathVariable(value = "taskId") String taskId);

    @PostMapping(path = "/v1/tasks/{taskId}/variables", headers = "Content-type=application/json")
    ResponseEntity<Void> createVariable(@PathVariable(value = "taskId") String taskId,
        @RequestBody CreateTaskVariablePayload createTaskVariablePayload);

    @PutMapping(value = "/v1/tasks/{taskId}/variables/{variableName}",
        headers = "Content-type=application/json")
    ResponseEntity<Void> updateVariable(@PathVariable(value = "taskId") String taskId,
        @PathVariable(value = "variableName") String variableName,
        @RequestBody UpdateTaskVariablePayload updateTaskVariablePayload);
}
