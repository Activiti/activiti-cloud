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

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.cloud.services.rest.api.TaskVariableController;
import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.cloud.services.rest.assemblers.TaskVariableInstanceResourceAssembler;
import org.activiti.runtime.api.cmd.SetTaskVariables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskVariableControllerImpl implements TaskVariableController {

    private final TaskVariableInstanceResourceAssembler variableResourceAssembler;

    private ResourcesAssembler resourcesAssembler;

    private SecurityAwareTaskService securityAwareTaskService;

    @Autowired
    public TaskVariableControllerImpl(TaskVariableInstanceResourceAssembler variableResourceAssembler,
                                      ResourcesAssembler resourcesAssembler,
                                      SecurityAwareTaskService securityAwareTaskService) {
        this.variableResourceAssembler = variableResourceAssembler;
        this.resourcesAssembler = resourcesAssembler;
        this.securityAwareTaskService = securityAwareTaskService;
    }

    @Override
    public Resources<VariableInstanceResource> getVariables(@PathVariable String taskId) {
        return resourcesAssembler.toResources(securityAwareTaskService.getVariableInstances(taskId), variableResourceAssembler);
    }

    @Override
    public Resources<VariableInstanceResource> getVariablesLocal(@PathVariable String taskId) {
       return resourcesAssembler.toResources(securityAwareTaskService.getLocalVariableInstances(taskId), variableResourceAssembler);
    }

    @Override
    public ResponseEntity<Void> setVariables(@PathVariable String taskId,
                                             @RequestBody SetTaskVariables setTaskVariablesCmd) {
        securityAwareTaskService.setTaskVariables(setTaskVariablesCmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> setVariablesLocal(@PathVariable String taskId,
                                                  @RequestBody SetTaskVariables setTaskVariablesCmd) {
        securityAwareTaskService.setTaskVariablesLocal(setTaskVariablesCmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
