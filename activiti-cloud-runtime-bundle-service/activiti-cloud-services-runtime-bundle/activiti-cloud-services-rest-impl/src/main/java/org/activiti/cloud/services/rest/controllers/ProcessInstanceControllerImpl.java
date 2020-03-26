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

import static java.util.Collections.emptyList;

import java.nio.charset.StandardCharsets;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceController;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceControllerImpl implements ProcessInstanceController {

    private final RepositoryService repositoryService;

    private final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    private final ProcessRuntime processRuntime;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessInstanceControllerImpl(RepositoryService repositoryService,
                                         ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                         ProcessInstanceResourceAssembler resourceAssembler,
                                         AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler,
                                         ProcessRuntime processRuntime,
                                         SpringPageConverter pageConverter) {
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.processRuntime = processRuntime;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<Resource<CloudProcessInstance>> getProcessInstances(Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processRuntime.processInstances(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                pageConverter.toSpringPage(pageable, processInstancePage),
                resourceAssembler);
    }

    @Override
    public Resource<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload startProcessPayload) {
        return resourceAssembler.toResource(processRuntime.start(startProcessPayload));
    }

    @Override
    public Resource<CloudProcessInstance> createProcessInstance(@RequestBody StartProcessPayload startProcessPayload) {
        return resourceAssembler.toResource(processRuntime.create(startProcessPayload));
    }

    @Override
    public Resource<CloudProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.processInstance(processInstanceId));
    }

    @Override
    public String getProcessDiagram(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = processRuntime.processInstance(processInstanceId);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        return new String(processDiagramGenerator.generateDiagram(bpmnModel,
                processRuntime
                        .processInstanceMeta(processInstance.getId())
                        .getActiveActivitiesIds(),
                emptyList()),
                StandardCharsets.UTF_8);
    }

    @Override
    public ResponseEntity<Void> sendSignal(@RequestBody SignalPayload cmd) {
        processRuntime.signal(cmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<CloudProcessInstance> suspend(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.suspend(ProcessPayloadBuilder.suspend(processInstanceId)));

    }

    @Override
    public Resource<CloudProcessInstance> resume(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.resume(ProcessPayloadBuilder.resume(processInstanceId)));
    }

    @Override
    public Resource<CloudProcessInstance> deleteProcessInstance(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.delete(ProcessPayloadBuilder.delete(processInstanceId)));
    }

    @Override
    public Resource<CloudProcessInstance> updateProcess(@PathVariable String processInstanceId,
                                                        @RequestBody UpdateProcessPayload payload) {
        if (payload != null) {
            payload.setProcessInstanceId(processInstanceId);

        }

        return resourceAssembler.toResource(processRuntime.update(payload));
    }

    @Override
    public PagedResources<Resource<CloudProcessInstance>> subprocesses(@PathVariable String processInstanceId,
                                                                       Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processRuntime.processInstances(pageConverter.toAPIPageable(pageable),
                ProcessPayloadBuilder.subprocesses(processInstanceId));

        return pagedResourcesAssembler.toResource(pageable,
                pageConverter.toSpringPage(pageable, processInstancePage),
                resourceAssembler);
    }

    @Override
    public Resource<CloudProcessInstance> sendStartMessage(@RequestBody StartMessagePayload startMessagePayload) {
        ProcessInstance processInstance = processRuntime.start(startMessagePayload);

        return resourceAssembler.toResource(processInstance);
    }

    @Override
    public ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload) {
        processRuntime.receive(receiveMessagePayload);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
