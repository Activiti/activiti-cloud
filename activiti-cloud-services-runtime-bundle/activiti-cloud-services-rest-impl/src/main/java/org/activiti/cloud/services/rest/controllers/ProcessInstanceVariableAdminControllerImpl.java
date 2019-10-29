/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableAdminController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceVariableAdminControllerImpl implements ProcessInstanceVariableAdminController {
    private final ProcessAdminRuntime processAdminRuntime;
    private final ProcessVariablesPayloadValidator processVariablesValidator;

    @Autowired
    public ProcessInstanceVariableAdminControllerImpl(ProcessAdminRuntime processAdminRuntime,
                                                      ProcessVariablesPayloadValidator processVariablesValidator) {
        this.processAdminRuntime = processAdminRuntime;
        this.processVariablesValidator = processVariablesValidator;
    }

    @Override
    public ResponseEntity<Void> updateVariables(@PathVariable String processInstanceId,
                                                @RequestBody SetProcessVariablesPayload setProcessVariablesPayload) {

        ProcessInstance processInstance = processAdminRuntime.processInstance(processInstanceId);
        setProcessVariablesPayload.setProcessInstanceId(processInstanceId);

        processVariablesValidator.checkPayloadVariables(setProcessVariablesPayload, processInstance.getProcessDefinitionId());

        processAdminRuntime.setVariables(setProcessVariablesPayload);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                                @RequestBody RemoveProcessVariablesPayload removeProcessVariablesPayload) {
        if (removeProcessVariablesPayload!=null) {
            removeProcessVariablesPayload.setProcessInstanceId(processInstanceId);

        }
        processAdminRuntime.removeVariables(removeProcessVariablesPayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
