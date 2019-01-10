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

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableController;
import org.activiti.cloud.services.rest.api.resources.VariableInstanceResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceVariableControllerImpl implements ProcessInstanceVariableController {

    private final ProcessInstanceVariableResourceAssembler variableResourceAssembler;
    private final ProcessRuntime processRuntime;
    private final ResourcesAssembler resourcesAssembler;

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
    public ProcessInstanceVariableControllerImpl(ProcessInstanceVariableResourceAssembler variableResourceAssembler,
                                                 ProcessRuntime processRuntime,
                                                 ResourcesAssembler resourcesAssembler) {
        this.variableResourceAssembler = variableResourceAssembler;
        this.processRuntime = processRuntime;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public Resources<VariableInstanceResource> getVariables(@PathVariable String processInstanceId) {
        return resourcesAssembler.toResources(processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                               .withProcessInstanceId(processInstanceId)
                                                                               .build()),
                                              variableResourceAssembler);
    }

    @Override
    public ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                             @RequestBody SetProcessVariablesPayload setProcessVariablesPayload) {
        setProcessVariablesPayload.setProcessInstanceId(processInstanceId);
        processRuntime.setVariables(setProcessVariablesPayload);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                                @RequestBody RemoveProcessVariablesPayload removeProcessVariablesPayload) {
        processRuntime.removeVariables(removeProcessVariablesPayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
