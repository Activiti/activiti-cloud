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
package org.activiti.cloud.services.rest.api;

import io.swagger.v3.oas.annotations.Parameter;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(
    value = "/v1/process-instances/{processInstanceId}/variables",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public interface ProcessInstanceVariableController {
    @RequestMapping(method = RequestMethod.GET)
    CollectionModel<EntityModel<CloudVariableInstance>> getVariables(
        @Parameter(description = "Enter the processInstanceId to get variables") @PathVariable String processInstanceId
    );

    @RequestMapping(method = RequestMethod.PUT)
    ResponseEntity<Void> updateVariables(
        @Parameter(
            description = "Enter the processInstanceId to update variables"
        ) @PathVariable String processInstanceId,
        @RequestBody SetProcessVariablesPayload setProcessVariablesPayload
    );
}
