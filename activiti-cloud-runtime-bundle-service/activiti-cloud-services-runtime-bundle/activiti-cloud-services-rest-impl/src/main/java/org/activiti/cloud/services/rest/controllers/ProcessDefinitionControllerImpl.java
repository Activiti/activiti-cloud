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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessDefinitionController;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionRepresentationModelAssembler;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class ProcessDefinitionControllerImpl implements ProcessDefinitionController {

    private final RepositoryService repositoryService;

    private final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    private final ProcessDefinitionRepresentationModelAssembler representationModelAssembler;

    private final ProcessRuntime processRuntime;

    private final AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessDefinitionControllerImpl(RepositoryService repositoryService,
                                           ProcessDiagramGeneratorWrapper processDiagramGenerator,
                                           ProcessDefinitionRepresentationModelAssembler representationModelAssembler,
                                           ProcessRuntime processRuntime,
                                           AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler,
                                           SpringPageConverter pageConverter) {
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.representationModelAssembler = representationModelAssembler;
        this.processRuntime = processRuntime;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedModel<EntityModel<CloudProcessDefinition>> getProcessDefinitions(Pageable pageable) {
        Page<ProcessDefinition> page = processRuntime.processDefinitions(pageConverter.toAPIPageable(pageable));
        return pagedCollectionModelAssembler.toModel(pageable,
                                                  pageConverter.toSpringPage(pageable, page),
                                                  representationModelAssembler);
    }

    @Override
    public EntityModel<CloudProcessDefinition> getProcessDefinition(@PathVariable String id) {
        return representationModelAssembler.toModel(processRuntime.processDefinition(id));
    }

    @Override
    public String getProcessModel(@PathVariable String id) {
        checkUserCanReadProcessDefinition(id);

        try (final InputStream resourceStream = repositoryService.getProcessModel(id)) {
            return new String(IoUtil.readInputStream(resourceStream,
                                                     null),
                              StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ActivitiException("Error occured while getting process model '" + id + "' : " + e.getMessage(),
                                        e);
        }
    }

    private void checkUserCanReadProcessDefinition(@PathVariable String id) {
        // check the user can see the process definition (which has same ID as BPMN model in engine)
        //will thrown an exception with the user is not authorized
        processRuntime.processDefinition(id);
    }

    @Override
    public String getBpmnModel(@PathVariable String id) {
        checkUserCanReadProcessDefinition(id);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        ObjectNode json = new BpmnJsonConverter().convertToJson(bpmnModel);
        return json.toString();
    }

    @Override
    public String getProcessDiagram(@PathVariable String id) {
        checkUserCanReadProcessDefinition(id);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        return new String(processDiagramGenerator.generateDiagram(bpmnModel),
                          StandardCharsets.UTF_8);
    }
}
