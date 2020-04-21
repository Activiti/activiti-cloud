/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.TaskVariableController;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.TaskVariableInstanceRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskVariableControllerImpl implements TaskVariableController {

    private final TaskVariableInstanceRepresentationModelAssembler variableRepresentationModelAssembler;
    private final CollectionModelAssembler resourcesAssembler;
    private final TaskRuntime taskRuntime;

    @Autowired
    public TaskVariableControllerImpl(TaskVariableInstanceRepresentationModelAssembler variableRepresentationModelAssembler,
                                      CollectionModelAssembler resourcesAssembler,
                                      TaskRuntime taskRuntime) {
        this.variableRepresentationModelAssembler = variableRepresentationModelAssembler;
        this.resourcesAssembler = resourcesAssembler;
        this.taskRuntime = taskRuntime;
    }

    @Override
    public CollectionModel<EntityModel<CloudVariableInstance>> getVariables(@PathVariable String taskId) {
        return resourcesAssembler.toCollectionModel(taskRuntime.variables(TaskPayloadBuilder.variables()
                                                                            .withTaskId(taskId)
                                                                            .build()),
                                              variableRepresentationModelAssembler);
    }

    @Override
    public ResponseEntity<Void> createVariable(@PathVariable String taskId,
                                               @RequestBody CreateTaskVariablePayload createTaskVariablePayload) {

        createTaskVariablePayload.setTaskId(taskId);
        taskRuntime.createVariable(createTaskVariablePayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> updateVariable(@PathVariable String taskId,
                                               @PathVariable String variableName,
                                               @RequestBody UpdateTaskVariablePayload updateTaskVariablePayload) {

        updateTaskVariablePayload.setTaskId(taskId);
        updateTaskVariablePayload.setName(variableName);
        taskRuntime.updateVariable(updateTaskVariablePayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
