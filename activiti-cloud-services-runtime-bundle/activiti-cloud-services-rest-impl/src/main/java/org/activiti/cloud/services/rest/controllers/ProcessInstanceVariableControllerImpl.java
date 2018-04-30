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

import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.rest.api.resources.ProcessVariableResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.engine.RuntimeService;

import org.activiti.cloud.services.rest.api.ProcessInstanceVariableController;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**

 */
@RestController
public class ProcessInstanceVariableControllerImpl implements ProcessInstanceVariableController {

    private final RuntimeService runtimeService;
    private final ProcessInstanceVariableResourceAssembler variableResourceBuilder;

    @Autowired
    public ProcessInstanceVariableControllerImpl(RuntimeService runtimeService,
                                                 ProcessInstanceVariableResourceAssembler variableResourceBuilder) {
        this.runtimeService = runtimeService;
        this.variableResourceBuilder = variableResourceBuilder;
    }

    @Override
    public Resources<ProcessVariableResource> getVariables(@PathVariable String processInstanceId) {
        List<VariableInstance> variableInstances = runtimeService.getVariableInstancesByExecutionIds(Collections.singleton(processInstanceId));

        List<ProcessVariableResource> resourcesList = new ArrayList<>();
        for(VariableInstance variableInstance:variableInstances){
            resourcesList.add(variableResourceBuilder.toResource(new ProcessInstanceVariable(
                    variableInstance.getProcessInstanceId(),variableInstance.getName(),variableInstance.getTypeName(),variableInstance.getValue(),variableInstance.getExecutionId())));
        }

        Resources<ProcessVariableResource> resources = new Resources<>(resourcesList);
        return resources;
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

        Resources<ProcessVariableResource> resources = new Resources<>(resourcesList);
        return resources;
    }
}
