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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.converter.ProcessDefinitionConverter;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.core.pageable.PageableRepositoryService;
import org.activiti.cloud.services.rest.api.ProcessDefinitionController;
import org.activiti.cloud.services.rest.api.resources.ProcessDefinitionResource;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionResourceAssembler;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessDefinitionControllerImpl implements ProcessDefinitionController {

    private final RepositoryService repositoryService;

    private final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    private final ProcessDefinitionConverter processDefinitionConverter;

    private final ProcessDefinitionResourceAssembler resourceAssembler;

    private final PageableRepositoryService pageableRepositoryService;

    private final SecurityPoliciesApplicationService securityService;

    private final AlfrescoPagedResourcesAssembler<ProcessDefinition> pagedResourcesAssembler;

    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(ActivitiObjectNotFoundException ex) {
        return ex.getMessage();
    }

    @Autowired
    public ProcessDefinitionControllerImpl(RepositoryService repositoryService,
                                           ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                           ProcessDefinitionConverter processDefinitionConverter,
                                           ProcessDefinitionResourceAssembler resourceAssembler,
                                           PageableRepositoryService pageableRepositoryService,
                                           SecurityPoliciesApplicationService securityPoliciesApplicationService,
                                           AlfrescoPagedResourcesAssembler<ProcessDefinition> pagedResourcesAssembler) {
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.processDefinitionConverter = processDefinitionConverter;
        this.resourceAssembler = resourceAssembler;
        this.pageableRepositoryService = pageableRepositoryService;
        this.securityService = securityPoliciesApplicationService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedResources<ProcessDefinitionResource> getProcessDefinitions(Pageable pageable) {
        Page<ProcessDefinition> page = pageableRepositoryService.getProcessDefinitions(pageable);
        return pagedResourcesAssembler.toResource(pageable,
                                                  page,
                                                  resourceAssembler);
    }

    @Override
    public ProcessDefinitionResource getProcessDefinition(@PathVariable String id) {

        org.activiti.engine.repository.ProcessDefinition processDefinition = retrieveProcessDefinition(id);
        return resourceAssembler.toResource(processDefinitionConverter.from(processDefinition));
    }

    private org.activiti.engine.repository.ProcessDefinition retrieveProcessDefinition(String id) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id);
        query = securityService.restrictProcessDefQuery(query,
                                                        SecurityPolicy.READ);
        org.activiti.engine.repository.ProcessDefinition processDefinition = query.singleResult();
        if (processDefinition == null) {
            throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + id + "'");
        }
        return processDefinition;
    }

    @Override
    public String getProcessModel(@PathVariable String id) {
        // first check the user can see the process definition (which has same ID as process model in engine)
        retrieveProcessDefinition(id);

        try (final InputStream resourceStream = repositoryService.getProcessModel(id)) {
            return new String(IoUtil.readInputStream(resourceStream,
                                                     null),
                              StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ActivitiException("Error occured while getting process model '" + id + "' : " + e.getMessage(),
                                        e);
        }
    }

    @Override
    public String getBpmnModel(@PathVariable String id) {
        // first check the user can see the process definition (which has same ID as BPMN model in engine)
        retrieveProcessDefinition(id);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        ObjectNode json = new BpmnJsonConverter().convertToJson(bpmnModel);
        return json.toString();
    }

    @Override
    public String getProcessDiagram(@PathVariable String id) {
        // first check the user can see the process definition (which has same ID as BPMN model in engine)
        retrieveProcessDefinition(id);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        return new String(processDiagramGenerator.generateDiagram(bpmnModel),
                          StandardCharsets.UTF_8);
    }
}
