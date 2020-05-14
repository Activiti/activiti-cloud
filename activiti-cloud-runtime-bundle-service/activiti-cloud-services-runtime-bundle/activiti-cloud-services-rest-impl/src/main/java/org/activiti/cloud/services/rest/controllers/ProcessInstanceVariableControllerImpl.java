/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableController;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ResourcesAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceVariableControllerImpl implements ProcessInstanceVariableController {

    private final ProcessInstanceVariableResourceAssembler variableResourceAssembler;
    private final ProcessRuntime processRuntime;
    private final ResourcesAssembler resourcesAssembler;
   
    @Autowired
    public ProcessInstanceVariableControllerImpl(ProcessInstanceVariableResourceAssembler variableResourceAssembler,
                                                 ProcessRuntime processRuntime,
                                                 ResourcesAssembler resourcesAssembler) {
        this.variableResourceAssembler = variableResourceAssembler;
        this.processRuntime = processRuntime;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public Resources<Resource<CloudVariableInstance>> getVariables(@PathVariable String processInstanceId) {
        return resourcesAssembler.toResources(processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                               .withProcessInstanceId(processInstanceId)
                                                                               .build()),
                                              variableResourceAssembler);
    }

    @Override
    public ResponseEntity<Void> setVariables(@PathVariable String processInstanceId,
                                             @RequestBody SetProcessVariablesPayload setProcessVariablesPayload) {
        
        if (setProcessVariablesPayload != null) {
            setProcessVariablesPayload.setProcessInstanceId(processInstanceId);
        }

        processRuntime.setVariables(setProcessVariablesPayload);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
