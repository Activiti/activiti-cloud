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
package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableAdminController;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceVariableAdminControllerImpl implements ProcessInstanceVariableAdminController {

    private final ProcessInstanceVariableRepresentationModelAssembler variableRepresentationModelAssembler;
    private final ProcessAdminRuntime processAdminRuntime;
    private final CollectionModelAssembler resourcesAssembler;

    @Autowired
    public ProcessInstanceVariableAdminControllerImpl(
        ProcessInstanceVariableRepresentationModelAssembler variableRepresentationModelAssembler,
        ProcessAdminRuntime processAdminRuntime,
        CollectionModelAssembler resourcesAssembler
    ) {
        this.variableRepresentationModelAssembler = variableRepresentationModelAssembler;
        this.processAdminRuntime = processAdminRuntime;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public CollectionModel<EntityModel<CloudVariableInstance>> getVariables(String processInstanceId) {
        return resourcesAssembler.toCollectionModel(
            processAdminRuntime.variables(
                ProcessPayloadBuilder.variables().withProcessInstanceId(processInstanceId).build()
            ),
            variableRepresentationModelAssembler
        );
    }

    @Override
    public ResponseEntity<Void> updateVariables(
        @PathVariable String processInstanceId,
        @RequestBody SetProcessVariablesPayload setProcessVariablesPayload
    ) {
        if (setProcessVariablesPayload != null) {
            setProcessVariablesPayload.setProcessInstanceId(processInstanceId);
        }

        processAdminRuntime.setVariables(setProcessVariablesPayload);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeVariables(
        @PathVariable String processInstanceId,
        @RequestBody RemoveProcessVariablesPayload removeProcessVariablesPayload
    ) {
        if (removeProcessVariablesPayload != null) {
            removeProcessVariablesPayload.setProcessInstanceId(processInstanceId);
        }
        processAdminRuntime.removeVariables(removeProcessVariablesPayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
