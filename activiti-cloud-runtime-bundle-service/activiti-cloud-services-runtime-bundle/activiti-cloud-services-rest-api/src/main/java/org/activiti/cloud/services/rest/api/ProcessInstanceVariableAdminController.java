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

package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/admin/v1/process-instances/{processInstanceId}/variables",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceVariableAdminController {

    /**
     * @param processInstanceId          id of process instance
     * @param setProcessVariablesPayload process variables payload
     * @return list of error messages, empty list if no error message
     */
    @PutMapping
    ResponseEntity<Void> updateVariables(@PathVariable String processInstanceId,
                                         @RequestBody SetProcessVariablesPayload setProcessVariablesPayload);
    
    /**
     * @param processInstanceId          id of process instance
     * @param removeProcessVariablesPayload process variables payload
     */
    @DeleteMapping
    ResponseEntity<Void> removeVariables(@PathVariable String processInstanceId,
                                         @RequestBody RemoveProcessVariablesPayload removeProcessVariablesPayload);

}
