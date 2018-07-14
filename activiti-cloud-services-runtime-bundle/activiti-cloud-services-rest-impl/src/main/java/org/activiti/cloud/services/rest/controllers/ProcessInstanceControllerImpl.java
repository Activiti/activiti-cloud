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

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.cloud.services.rest.api.ProcessInstanceController;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.activiti.runtime.api.NotFoundException;
import org.activiti.runtime.api.cmd.SendSignal;
import org.activiti.runtime.api.cmd.StartProcess;
import org.activiti.runtime.api.cmd.impl.ResumeProcessImpl;
import org.activiti.runtime.api.cmd.impl.SuspendProcessImpl;
import org.activiti.runtime.api.model.FluentProcessInstance;
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

    private final AlfrescoPagedResourcesAssembler<org.activiti.runtime.api.model.ProcessInstance> pagedResourcesAssembler;

    private final SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

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
                                         AlfrescoPagedResourcesAssembler<org.activiti.runtime.api.model.ProcessInstance> pagedResourcesAssembler,
                                         SecurityAwareProcessInstanceService securityAwareProcessInstanceService) {
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
    }

    @Override
    public PagedResources<ProcessInstanceResource> getProcessInstances(Pageable pageable) {
        return pagedResourcesAssembler.toResource(pageable,
                                                  securityAwareProcessInstanceService.getAuthorizedProcessInstances(pageable),
                                                  resourceAssembler);
    }

    @Override
    public ProcessInstanceResource startProcess(@RequestBody StartProcess cmd) {

        return resourceAssembler.toResource(securityAwareProcessInstanceService.startProcess(cmd));
    }

    @Override
    public ProcessInstanceResource getProcessInstanceById(@PathVariable String processInstanceId) {
        return resourceAssembler.toResource(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstanceId));
    }

    @Override
    public String getProcessDiagram(@PathVariable String processInstanceId) {
        FluentProcessInstance processInstance = securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstanceId);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        return new String(processDiagramGenerator.generateDiagram(bpmnModel,
                                                                  processInstance.activeActivityIds(),
                                                                  emptyList()),
                          StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> sendSignal(@RequestBody SendSignal cmd) {
        securityAwareProcessInstanceService.signal(cmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> suspend(@PathVariable String processInstanceId) {
        securityAwareProcessInstanceService.suspend(new SuspendProcessImpl(processInstanceId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> activate(@PathVariable String processInstanceId) {
        securityAwareProcessInstanceService.activate(new ResumeProcessImpl(processInstanceId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public void deleteProcessInstance(@PathVariable String processInstanceId) {
        securityAwareProcessInstanceService.deleteProcessInstance(processInstanceId);
    }

}
