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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.rest.api.ProcessInstanceController;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.image.ProcessDiagramGenerator;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.rest.api.resources.assembler.ProcessInstanceResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceControllerImpl implements ProcessInstanceController {

    private ProcessEngineWrapper processEngine;

    private final RepositoryService repositoryService;

    private final ProcessDiagramGenerator processDiagramGenerator;

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final SecurityPoliciesApplicationService securityService;

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }


    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(ActivitiObjectNotFoundException ex) {
        return ex.getMessage();
    }

    @Autowired
    public ProcessInstanceControllerImpl(ProcessEngineWrapper processEngine,
                                         RepositoryService repositoryService,
                                         ProcessDiagramGenerator processDiagramGenerator,
                                         ProcessInstanceResourceAssembler resourceAssembler,
                                         SecurityPoliciesApplicationService securityService) {
        this.processEngine = processEngine;
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.resourceAssembler = resourceAssembler;
        this.securityService = securityService;
    }

    @Override
    public PagedResources<ProcessInstanceResource> getProcessInstances(Pageable pageable,
                                                                       PagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler) {
        return pagedResourcesAssembler.toResource(processEngine.getProcessInstances(pageable),
                                                  resourceAssembler);
    }

    @Override
    public Resource<ProcessInstance> startProcess(@RequestBody StartProcessInstanceCmd cmd) {

        return resourceAssembler.toResource(processEngine.startProcess(cmd));
    }

    @Override
    public Resource<ProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = processEngine.getProcessInstanceById(processInstanceId);
        if(processInstance == null || !securityService.canRead(processInstance.getProcessDefinitionKey())){
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + processInstanceId + "'");
        }
        return resourceAssembler.toResource(processInstance);
    }

    @Override
    public String getProcessDiagram(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = processEngine.getProcessInstanceById(processInstanceId);
        if (processInstance == null || !securityService.canRead(processInstance.getProcessDefinitionKey())) {
            throw new ActivitiObjectNotFoundException("Unable to find process instance for the given id:'" + processInstanceId + "'");
        }
        List<String> activityIds = processEngine.getActiveActivityIds(processInstanceId);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        String activityFontName = processDiagramGenerator.getDefaultActivityFontName();
        String labelFontName = processDiagramGenerator.getDefaultLabelFontName();
        String annotationFontName = processDiagramGenerator.getDefaultAnnotationFontName();
        try (final InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel,
                                                                                     activityIds,
                                                                                     Collections.emptyList(),
                                                                                     activityFontName,
                                                                                     labelFontName,
                                                                                     annotationFontName)) {
            return new String(IoUtil.readInputStream(imageStream,
                                                     null),
                              StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ActivitiException("Error occured while getting process diagram '" + processInstanceId + "' : " + e.getMessage(),
                                        e);
        }
    }

    @Override
    public ResponseEntity<Void> sendSignal(@RequestBody SignalProcessInstancesCmd cmd) {
        processEngine.signal(cmd);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> suspend(@PathVariable String processInstanceId) {
        processEngine.suspend(new SuspendProcessInstanceCmd(processInstanceId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> activate(@PathVariable String processInstanceId) {
        processEngine.activate(new ActivateProcessInstanceCmd(processInstanceId));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
