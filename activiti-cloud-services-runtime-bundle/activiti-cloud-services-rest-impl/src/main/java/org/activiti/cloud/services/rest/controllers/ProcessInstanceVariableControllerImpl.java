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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableController;
import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**

 */
@RestController
public class ProcessInstanceVariableControllerImpl implements ProcessInstanceVariableController {

    private final RuntimeService runtimeService;
    private final ProcessInstanceVariableResourceAssembler variableResourceBuilder;
    private final ProcessEngineWrapper processEngine;
    private final SecurityAwareProcessInstanceService processInstanceService;


    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(ActivitiObjectNotFoundException ex) {
        return ex.getMessage();
    }

    @Autowired
    public ProcessInstanceVariableControllerImpl(RuntimeService runtimeService,
                                                 ProcessInstanceVariableResourceAssembler variableResourceBuilder,
                                                 ProcessEngineWrapper processEngine,
                                                 SecurityAwareProcessInstanceService processInstanceService) {
        this.runtimeService = runtimeService;
        this.variableResourceBuilder = variableResourceBuilder;
        this.processEngine = processEngine;
        this.processInstanceService = processInstanceService;
    }

    @Override
    public Resources<ProcessVariableResource> getVariables(@PathVariable String processInstanceId) {
        List<VariableInstance> variableInstances = runtimeService.getVariableInstancesByExecutionIds(Collections.singleton(processInstanceId));

        List<ProcessVariableResource> resourcesList = new ArrayList<>();
        for(VariableInstance variableInstance:variableInstances){
            resourcesList.add(variableResourceBuilder.toResource(new ProcessInstanceVariable(
                    variableInstance.getProcessInstanceId(),variableInstance.getName(),variableInstance.getTypeName(),variableInstance.getValue(),variableInstance.getExecutionId())));
        }

        return new Resources<>(resourcesList);
    }

    @Override
    public Resources<ProcessVariableResource> getVariablesLocal(@PathVariable String processInstanceId) {
        Map<String,VariableInstance> variableInstancesMap = runtimeService.getVariableInstancesLocal(processInstanceId);
        List<VariableInstance> variableInstances = new ArrayList<>();
        if(variableInstancesMap!=null){
            variableInstances.addAll(variableInstancesMap.values());
        }
        List<ProcessVariableResource> resourcesList = new ArrayList<>();
        for(VariableInstance variableInstance:variableInstances){
            resourcesList.add(variableResourceBuilder.toResource(new ProcessInstanceVariable(
                    variableInstance.getProcessInstanceId(),variableInstance.getName(),variableInstance.getTypeName(),variableInstance.getValue(),variableInstance.getExecutionId())));
        }

        return new Resources<>(resourcesList);
    }


    @Override
    public ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                             @RequestBody SetProcessVariablesCmd setProcessVariablesCmd) {
        processInstanceService.setProcessVariables(setProcessVariablesCmd);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                                @RequestBody RemoveProcessVariablesCmd removeProcessVariablesCmd) {
        this.processEngine.removeProcessVariables(removeProcessVariablesCmd);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
