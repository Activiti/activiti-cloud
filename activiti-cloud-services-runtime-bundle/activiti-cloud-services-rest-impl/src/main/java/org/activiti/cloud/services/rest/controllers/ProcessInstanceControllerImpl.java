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

import java.nio.charset.StandardCharsets;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceController;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Collections.emptyList;

@RestController
public class ProcessInstanceControllerImpl implements ProcessInstanceController {

    private final RepositoryService repositoryService;

    private final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    private final ProcessRuntime processRuntime;

    private final SpringPageConverter pageConverter;

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler({ActivitiObjectNotFoundException.class, NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(RuntimeException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ActivitiInterchangeInfoNotFoundException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
        public String handleActivitiInterchangeInfoNotFoundException(ActivitiInterchangeInfoNotFoundException ex) {
        return ex.getMessage();
    }

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
    public PagedResources<ProcessInstanceResource> getProcessInstances(Pageable pageable) {
        Page<ProcessInstance> processInstancePage = processRuntime.processInstances(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable, processInstancePage),
                                                  resourceAssembler);
    }

    @Override
    public ProcessInstanceResource startProcess(@RequestBody StartProcessPayload startProcessPayload) {
        return resourceAssembler.toResource(processRuntime.start(startProcessPayload));
    }

    @Override
    public ProcessInstanceResource getProcessInstanceById(@PathVariable String processInstanceId) {
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
    @Transactional
    public ResponseEntity<Void> sendSignal(@RequestBody SignalPayload cmd) {
        processRuntime.signal(cmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ProcessInstanceResource suspend(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.suspend(ProcessPayloadBuilder.suspend(processInstanceId)));

    }

    @Override
    public ProcessInstanceResource activate(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.resume(ProcessPayloadBuilder.resume(processInstanceId)));
    }

    @Override
    public ProcessInstanceResource deleteProcessInstance(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(processRuntime.delete(ProcessPayloadBuilder.delete(processInstanceId)));
    }

}
