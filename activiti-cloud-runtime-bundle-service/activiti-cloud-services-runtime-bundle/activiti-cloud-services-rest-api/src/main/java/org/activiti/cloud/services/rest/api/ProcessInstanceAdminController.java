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
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(
    value = "/admin/v1/process-instances",
    produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE }
)
public interface ProcessInstanceAdminController {
    @RequestMapping(method = RequestMethod.GET)
    PagedModel<EntityModel<CloudProcessInstance>> getProcessInstances(Pageable pageable);

    @RequestMapping(method = RequestMethod.POST)
    EntityModel<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload cmd);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    EntityModel<CloudProcessInstance> getProcessInstanceById(
        @Parameter(
            description = "Enter the processInstanceId to get process instance by id candidate groups"
        ) @PathVariable String processInstanceId
    );

    @RequestMapping(method = RequestMethod.POST, value = "{processInstanceId}/suspend")
    EntityModel<CloudProcessInstance> suspend(
        @Parameter(description = "Enter the processInstanceId to suspend") @PathVariable String processInstanceId
    );

    @RequestMapping(method = RequestMethod.POST, value = "{processInstanceId}/resume")
    EntityModel<CloudProcessInstance> resume(
        @Parameter(description = "Enter the processInstanceId to resume") @PathVariable String processInstanceId
    );

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.DELETE)
    EntityModel<CloudProcessInstance> deleteProcessInstance(
        @Parameter(description = "Enter the processInstanceId to delete") @PathVariable String processInstanceId
    );

    @RequestMapping(value = "/{processInstanceId}/destroy", method = RequestMethod.DELETE)
    ResponseEntity<Void> destroyProcessInstance(
        @Parameter(
            description = "Enter the processInstanceId to delete a process instance"
        ) @PathVariable String processInstanceId
    );

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.PUT)
    EntityModel<CloudProcessInstance> updateProcess(
        @Parameter(description = "Enter the processInstanceId to update process") @PathVariable(
            "processInstanceId"
        ) String processInstanceId,
        @RequestBody UpdateProcessPayload payload
    );

    @RequestMapping(value = "/{processInstanceId}/subprocesses", method = RequestMethod.GET)
    PagedModel<EntityModel<CloudProcessInstance>> subprocesses(
        @PathVariable("processInstanceId") String processInstanceId,
        Pageable pageable
    );

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    EntityModel<CloudProcessInstance> start(@RequestBody StartMessagePayload startMessagePayload);

    @RequestMapping(value = "/message", method = RequestMethod.PUT)
    ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload);
}
