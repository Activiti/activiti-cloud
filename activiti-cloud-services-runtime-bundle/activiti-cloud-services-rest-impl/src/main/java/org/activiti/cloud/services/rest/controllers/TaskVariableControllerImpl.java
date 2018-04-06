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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.cloud.services.api.model.TaskVariable;
import org.activiti.cloud.services.rest.api.resources.TaskVariableResource;
import org.activiti.engine.TaskService;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.rest.api.TaskVariableController;
import org.activiti.cloud.services.rest.assemblers.TaskVariableResourceAssembler;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskVariableControllerImpl implements TaskVariableController {

    private ProcessEngineWrapper processEngine;

    private final TaskService taskService;

    private final TaskVariableResourceAssembler variableResourceBuilder;

    @Autowired
    public TaskVariableControllerImpl(ProcessEngineWrapper processEngine,
                                      TaskService taskService,
                                      TaskVariableResourceAssembler variableResourceBuilder) {
        this.processEngine = processEngine;
        this.taskService = taskService;
        this.variableResourceBuilder = variableResourceBuilder;
    }

    @Override
    public Resources<TaskVariableResource> getVariables(@PathVariable String taskId) {
        Map<String, Object> variables = taskService.getVariables(taskId);
        Map<String, VariableInstance> variableInstancesMap = taskService.getVariableInstances(taskId);
        List<VariableInstance> variableInstances = new ArrayList<>();
        if(variableInstancesMap!=null){
            variableInstances.addAll(variableInstancesMap.values());
        }
        List<TaskVariableResource> resourcesList = new ArrayList<>();
        for(VariableInstance variableInstance:variableInstances){
            resourcesList.add(variableResourceBuilder.toResource(new TaskVariable(
                    variableInstance.getTaskId(),variableInstance.getName(),variableInstance.getTypeName(),
                    variableInstance.getValue(),variableInstance.getExecutionId(),
                    TaskVariable.TaskVariableScope.GLOBAL)));
        }

        Resources<TaskVariableResource> resources = new Resources<>(resourcesList);
        return resources;
    }

    @Override
    public Resources<TaskVariableResource> getVariablesLocal(@PathVariable String taskId) {
        Map<String, Object> variables = taskService.getVariablesLocal(taskId);
        Map<String, VariableInstance> variableInstancesMap = taskService.getVariableInstancesLocal(taskId);
        List<VariableInstance> variableInstances = new ArrayList<>();
        if(variableInstancesMap!=null){
            variableInstances.addAll(variableInstancesMap.values());
        }
        List<TaskVariableResource> resourcesList = new ArrayList<>();
        for(VariableInstance variableInstance:variableInstances){
            resourcesList.add(variableResourceBuilder.toResource(new TaskVariable(
                    variableInstance.getTaskId(),variableInstance.getName(),variableInstance.getTypeName(),
                    variableInstance.getValue(),variableInstance.getExecutionId(),
                    TaskVariable.TaskVariableScope.LOCAL)));
        }

        Resources<TaskVariableResource> resources = new Resources<>(resourcesList);
        return resources;
    }

    @Override
    public ResponseEntity<Void> setVariables(@PathVariable String taskId,
                                             @RequestBody(required = true) SetTaskVariablesCmd setTaskVariablesCmd) {
        processEngine.setTaskVariables(setTaskVariablesCmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> setVariablesLocal(@PathVariable String taskId,
                                                  @RequestBody(
                                                          required = true) SetTaskVariablesCmd setTaskVariablesCmd) {
        processEngine.setTaskVariablesLocal(setTaskVariablesCmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
