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

import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.core.ProcessVariablesPayloadConverter;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.events.services.CloudProcessDeletedService;
import org.activiti.cloud.services.rest.api.ProcessInstanceAdminController;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceRepresentationModelAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceAdminControllerImpl implements ProcessInstanceAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceAdminControllerImpl.class);

    private final Set<ProcessInstanceStatus> deleteStatuses = Set.of(
        ProcessInstanceStatus.COMPLETED,
        ProcessInstanceStatus.CANCELLED
    );

    private final String DELETE_PROCESS_NOT_ALLOWED =
        "Process Instance %s is not in status: " +
        String.join(", ", deleteStatuses.stream().map(Enum::name).collect(Collectors.toList()));

    private final ProcessInstanceRepresentationModelAssembler representationModelAssembler;

    private final AlfrescoPagedModelAssembler<ProcessInstance> pagedCollectionModelAssembler;

    private final ProcessAdminRuntime processAdminRuntime;

    private final SpringPageConverter pageConverter;

    private final ProcessVariablesPayloadConverter processVariablesPayloadConverter;

    private final CloudProcessDeletedService cloudProcessDeletedService;

    public ProcessInstanceAdminControllerImpl(
        ProcessInstanceRepresentationModelAssembler representationModelAssembler,
        AlfrescoPagedModelAssembler<ProcessInstance> pagedCollectionModelAssembler,
        ProcessAdminRuntime processAdminRuntime,
        SpringPageConverter pageConverter,
        ProcessVariablesPayloadConverter processVariablesPayloadConverter,
        CloudProcessDeletedService cloudProcessDeletedService
    ) {
        this.representationModelAssembler = representationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.processAdminRuntime = processAdminRuntime;
        this.pageConverter = pageConverter;
        this.processVariablesPayloadConverter = processVariablesPayloadConverter;
        this.cloudProcessDeletedService = cloudProcessDeletedService;
    }

    @Override
    public PagedModel<EntityModel<CloudProcessInstance>> getProcessInstances(Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processAdminRuntime.processInstances(
            pageConverter.toAPIPageable(pageable)
        );
        return pagedCollectionModelAssembler.toModel(
            pageable,
            pageConverter.toSpringPage(pageable, processInstancePage),
            representationModelAssembler
        );
    }

    @Override
    public EntityModel<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload startProcessPayload) {
        StartProcessPayload convertedStartProcessPayload = processVariablesPayloadConverter.convert(
            startProcessPayload
        );

        return representationModelAssembler.toModel(processAdminRuntime.start(convertedStartProcessPayload));
    }

    @Override
    public EntityModel<CloudProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId) {
        return representationModelAssembler.toModel(processAdminRuntime.processInstance(processInstanceId));
    }

    @Override
    public EntityModel<CloudProcessInstance> resume(@PathVariable String processInstanceId) {
        return representationModelAssembler.toModel(
            processAdminRuntime.resume(ProcessPayloadBuilder.resume(processInstanceId))
        );
    }

    @Override
    public EntityModel<CloudProcessInstance> suspend(@PathVariable String processInstanceId) {
        return representationModelAssembler.toModel(
            processAdminRuntime.suspend(ProcessPayloadBuilder.suspend(processInstanceId))
        );
    }

    @Override
    public EntityModel<CloudProcessInstance> deleteProcessInstance(@PathVariable String processInstanceId) {
        return representationModelAssembler.toModel(
            processAdminRuntime.delete(ProcessPayloadBuilder.delete(processInstanceId))
        );
    }

    @Override
    public ResponseEntity<Void> destroyProcessInstance(@PathVariable String processInstanceId) {
        try {
            ProcessInstance processInstance = processAdminRuntime.processInstance(processInstanceId);
            if (processInstance != null && !deleteStatuses.contains(processInstance.getStatus())) {
                throw new IllegalStateException(String.format(DELETE_PROCESS_NOT_ALLOWED, processInstanceId));
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Process Instance " + processInstanceId + " not found. Sending PROCESS_DELETE event.");
        }

        cloudProcessDeletedService.sendDeleteEvent(processInstanceId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<CloudProcessInstance> updateProcess(
        @PathVariable String processInstanceId,
        @RequestBody UpdateProcessPayload payload
    ) {
        if (payload != null) {
            payload.setProcessInstanceId(processInstanceId);
        }
        return representationModelAssembler.toModel(processAdminRuntime.update(payload));
    }

    @Override
    public PagedModel<EntityModel<CloudProcessInstance>> subprocesses(
        @PathVariable String processInstanceId,
        Pageable pageable
    ) {
        Page<ProcessInstance> processInstancePage = processAdminRuntime.processInstances(
            pageConverter.toAPIPageable(pageable),
            ProcessPayloadBuilder.subprocesses(processInstanceId)
        );

        return pagedCollectionModelAssembler.toModel(
            pageable,
            pageConverter.toSpringPage(pageable, processInstancePage),
            representationModelAssembler
        );
    }

    @Override
    public EntityModel<CloudProcessInstance> start(@RequestBody StartMessagePayload startMessagePayload) {
        startMessagePayload = processVariablesPayloadConverter.convert(startMessagePayload);

        ProcessInstance processInstance = processAdminRuntime.start(startMessagePayload);

        return representationModelAssembler.toModel(processInstance);
    }

    @Override
    public ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload) {
        processAdminRuntime.receive(receiveMessagePayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
