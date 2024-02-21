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
import org.activiti.api.process.model.payloads.*;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public interface ProcessInstanceController {
    @GetMapping("/v1/process-instances")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudProcessInstance>> getProcessInstances(Pageable pageable);

    @PostMapping(path = "/v1/process-instances", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload cmd);

    @Deprecated
    @PostMapping(value = "/v1/process-instances/{processInstanceId}/start", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> startCreatedProcess(
        @PathVariable(value = "processInstanceId") String processInstanceId,
        @RequestBody(required = false) StartProcessPayload payload
    );

    @Deprecated
    @PostMapping(value = "/v1/process-instances/create", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> createProcessInstance(@RequestBody CreateProcessInstancePayload cmd);

    @GetMapping(value = "/v1/process-instances/{processInstanceId}")
    EntityModel<CloudProcessInstance> getProcessInstanceById(
        @PathVariable(value = "processInstanceId") String processInstanceId
    );

    @GetMapping(value = "/v1/process-instances/{processInstanceId}/model", produces = "image/svg+xml")
    @ResponseBody
    String getProcessDiagram(@PathVariable(value = "processInstanceId") String processInstanceId);

    @PostMapping(value = "/v1/process-instances/signal", consumes = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sendSignal(@RequestBody SignalPayload signalPayload);

    @PostMapping(value = "/v1/process-instances/message", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> sendStartMessage(@RequestBody StartMessagePayload startMessagePayload);

    @PutMapping(value = "/v1/process-instances/message", consumes = APPLICATION_JSON_VALUE)
    ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload);

    @PostMapping(value = "/v1/process-instances/{processInstanceId}/suspend", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> suspend(
        @Parameter(description = "Enter the processInstanceId to suspend") @PathVariable(
            value = "processInstanceId"
        ) String processInstanceId
    );

    @PostMapping(value = "/v1/process-instances/{processInstanceId}/resume", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> resume(
        @Parameter(description = "Enter the processInstanceId to resume") @PathVariable(
            value = "processInstanceId"
        ) String processInstanceId
    );

    @DeleteMapping(value = "/v1/process-instances/{processInstanceId}")
    EntityModel<CloudProcessInstance> deleteProcessInstance(
        @Parameter(description = "Enter the processInstanceId to delete") @PathVariable(
            value = "processInstanceId"
        ) String processInstanceId
    );

    @PutMapping(value = "/v1/process-instances/{processInstanceId}", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudProcessInstance> updateProcess(
        @Parameter(description = "Enter the processInstanceId to update") @PathVariable(
            value = "processInstanceId"
        ) String processInstanceId,
        @RequestBody UpdateProcessPayload payload
    );

    @GetMapping(value = "/v1/process-instances/{processInstanceId}/subprocesses")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudProcessInstance>> subprocesses(
        @PathVariable(value = "processInstanceId") String processInstanceId,
        Pageable pageable
    );
}
