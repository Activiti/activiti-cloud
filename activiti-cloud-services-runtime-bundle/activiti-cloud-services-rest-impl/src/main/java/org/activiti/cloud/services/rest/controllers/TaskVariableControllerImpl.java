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

import java.util.Map;

import org.activiti.cloud.services.api.model.TaskVariables;
import org.activiti.engine.TaskService;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.rest.api.TaskVariableController;
import org.activiti.cloud.services.rest.assemblers.TaskVariablesResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskVariableControllerImpl implements TaskVariableController {

    private ProcessEngineWrapper processEngine;

    private final TaskService taskService;

    private final TaskVariablesResourceAssembler variableResourceBuilder;

    @Autowired
    public TaskVariableControllerImpl(ProcessEngineWrapper processEngine,
                                      TaskService taskService,
                                      TaskVariablesResourceAssembler variableResourceBuilder) {
        this.processEngine = processEngine;
        this.taskService = taskService;
        this.variableResourceBuilder = variableResourceBuilder;
    }

    @Override
    public Resource<Map<String, Object>> getVariables(@PathVariable String taskId) {
        Map<String, Object> variables = taskService.getVariables(taskId);
        return variableResourceBuilder.toResource(new TaskVariables(taskId, variables, TaskVariables.TaskVariableScope.GLOBAL));
    }

    @Override
    public Resource<Map<String, Object>> getVariablesLocal(@PathVariable String taskId) {
        Map<String, Object> variables = taskService.getVariablesLocal(taskId);
        return variableResourceBuilder.toResource(new TaskVariables(taskId, variables, TaskVariables.TaskVariableScope.LOCAL));
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
