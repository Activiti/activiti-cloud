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

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.core.ProcessVariablesPayloadConverter;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceAdminController;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceAdminControllerImpl implements ProcessInstanceAdminController {

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    private final ProcessAdminRuntime processAdminRuntime;

    private final SpringPageConverter pageConverter;

    private final ProcessVariablesPayloadConverter variablesPayloadConverter;

    public ProcessInstanceAdminControllerImpl(ProcessInstanceResourceAssembler resourceAssembler,
                                              AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler,
                                              ProcessAdminRuntime processAdminRuntime,
                                              SpringPageConverter pageConverter,
                                              ProcessVariablesPayloadConverter variablesPayloadConverter) {
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.processAdminRuntime = processAdminRuntime;
        this.pageConverter = pageConverter;
        this.variablesPayloadConverter = variablesPayloadConverter;
    }

    @Override
    public PagedResources<Resource<CloudProcessInstance>> getProcessInstances(Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processAdminRuntime.processInstances(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable, processInstancePage),
                                                  resourceAssembler);
    }

    @Override
    public Resource<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload startProcessPayload) {
        StartProcessPayload convertedStartProcessPayload = variablesPayloadConverter.convert(startProcessPayload);

        return resourceAssembler.toResource(processAdminRuntime.start(convertedStartProcessPayload));
    }

    @Override
    public Resource<CloudProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processAdminRuntime.processInstance(processInstanceId));
    }

    @Override
    public Resource<CloudProcessInstance> resume(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processAdminRuntime.resume(ProcessPayloadBuilder.resume(processInstanceId)));
    }

	@Override
	public Resource<CloudProcessInstance> suspend(@PathVariable String processInstanceId) {
		return resourceAssembler.toResource(processAdminRuntime.suspend(ProcessPayloadBuilder.suspend(processInstanceId)));
	}

    @Override
    public Resource<CloudProcessInstance> deleteProcessInstance(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processAdminRuntime.delete(ProcessPayloadBuilder.delete(processInstanceId)));
    }

    @Override
    public Resource<CloudProcessInstance> updateProcess(@PathVariable String processInstanceId,
                                                        @RequestBody UpdateProcessPayload payload) {
        if (payload!=null) {
            payload.setProcessInstanceId(processInstanceId);

        }
        return resourceAssembler.toResource(processAdminRuntime.update(payload));
    }

    @Override
    public PagedResources<Resource<CloudProcessInstance>> subprocesses(@PathVariable String processInstanceId,
                                                                       Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processAdminRuntime.processInstances(pageConverter.toAPIPageable(pageable),
                                                                                         ProcessPayloadBuilder.subprocesses(processInstanceId));

        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable, processInstancePage),
                                                  resourceAssembler);
    }

    @Override
    public Resource<CloudProcessInstance> start(@RequestBody StartMessagePayload startMessagePayload) {
        startMessagePayload = variablesPayloadConverter.convert(startMessagePayload);

        ProcessInstance processInstance = processAdminRuntime.start(startMessagePayload);

        return resourceAssembler.toResource(processInstance);
    }

    @Override
    public ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload) {
        processAdminRuntime.receive(receiveMessagePayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
