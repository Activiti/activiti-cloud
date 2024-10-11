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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.services.core.ProcessDefinitionService;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessDefinitionController;
import org.activiti.cloud.services.rest.assemblers.ExtendedCloudProcessDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionRepresentationModelAssembler;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.spring.process.CachingProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.Mapping.SourceMappingType;
import org.activiti.spring.process.model.ProcessConstantsMapping;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
public class ProcessDefinitionControllerImpl implements ProcessDefinitionController {

    private final RepositoryService repositoryService;

    private final ProcessDiagramGeneratorWrapper processDiagramGenerator;

    private final ProcessDefinitionRepresentationModelAssembler representationModelAssembler;

    private final ExtendedCloudProcessDefinitionRepresentationModelAssembler extendedCloudProcessDefinitionRepresentationModelAssembler;

    private final ProcessRuntime processRuntime;

    private final AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    private final ProcessDefinitionService processDefinitionService;

    private final CachingProcessExtensionService cachingProcessExtensionService;

    @Autowired
    public ProcessDefinitionControllerImpl(
        RepositoryService repositoryService,
        ProcessDiagramGeneratorWrapper processDiagramGenerator,
        ProcessDefinitionRepresentationModelAssembler representationModelAssembler,
        ExtendedCloudProcessDefinitionRepresentationModelAssembler extendedCloudProcessDefinitionRepresentationModelAssembler,
        ProcessRuntime processRuntime,
        AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler,
        SpringPageConverter pageConverter,
        ProcessDefinitionService processDefinitionService,
        CachingProcessExtensionService cachingProcessExtensionService
    ) {
        this.repositoryService = repositoryService;
        this.processDiagramGenerator = processDiagramGenerator;
        this.representationModelAssembler = representationModelAssembler;
        this.extendedCloudProcessDefinitionRepresentationModelAssembler =
            extendedCloudProcessDefinitionRepresentationModelAssembler;
        this.processRuntime = processRuntime;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
        this.processDefinitionService = processDefinitionService;
        this.cachingProcessExtensionService = cachingProcessExtensionService;
    }

    @Override
    public PagedModel<EntityModel<ExtendedCloudProcessDefinition>> getProcessDefinitions(
        @RequestParam(required = false, defaultValue = "") List<String> include,
        Pageable pageable
    ) {
        Page<ProcessDefinition> page = processDefinitionService.getProcessDefinitions(
            pageConverter.toAPIPageable(pageable),
            include
        );
        return pagedCollectionModelAssembler.toModel(
            pageable,
            pageConverter.toSpringPage(pageable, page),
            extendedCloudProcessDefinitionRepresentationModelAssembler
        );
    }

    @Override
    public EntityModel<CloudProcessDefinition> getProcessDefinition(@PathVariable String id) {
        return representationModelAssembler.toModel(processRuntime.processDefinition(id));
    }

    @Override
    public String getProcessModel(@PathVariable String id) {
        checkUserCanReadProcessDefinition(id);

        try (final InputStream resourceStream = repositoryService.getProcessModel(id)) {
            return new String(IoUtil.readInputStream(resourceStream, null), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ActivitiException(
                "Error occured while getting process model '" + id + "' : " + e.getMessage(),
                e
            );
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
        return new String(processDiagramGenerator.generateDiagram(bpmnModel), StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, Object> getProcessModelStaticValuesMappingForStartEvent(String id) {
        Map<String, Object> result = new HashMap<>();
        ExtensionsStartEventId extensionsStartEventId = getProcessExtensionsForStartEvent(id, true);
        if (
            extensionsStartEventId != null &&
            extensionsStartEventId.extensions() != null &&
            extensionsStartEventId.id() != null
        ) {
            ProcessVariablesMapping startEventMappings = extensionsStartEventId
                .extensions()
                .getMappings()
                .get(extensionsStartEventId.id());

            if (startEventMappings != null) {
                startEventMappings
                    .getInputs()
                    .forEach((input, mapping) -> {
                        if (SourceMappingType.VALUE.equals(mapping.getType())) {
                            result.put(input, mapping.getValue());
                        }
                    });
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getProcessModelConstantValuesForStartEvent(String id) {
        Map<String, Object> result = new HashMap<>();
        ExtensionsStartEventId extensionsStartEventId = getProcessExtensionsForStartEvent(id, false);
        if (
            extensionsStartEventId != null &&
            extensionsStartEventId.extensions() != null &&
            extensionsStartEventId.id() != null
        ) {
            ProcessConstantsMapping startEventConstants = extensionsStartEventId
                .extensions()
                .getConstantForFlowElement(extensionsStartEventId.id());

            if (startEventConstants != null) {
                startEventConstants.keySet().forEach(key -> result.put(key, startEventConstants.get(key).getValue()));
            }
        }

        return result;
    }

    private ExtensionsStartEventId getProcessExtensionsForStartEvent(String id, boolean formRequired) {
        checkUserCanReadProcessDefinition(id);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        Process process = bpmnModel.getMainProcess();

        if (!formRequired || bpmnModel.getStartFormKey(process.getId()) != null) {
            Optional<FlowElement> startEvent = process
                .getFlowElements()
                .stream()
                .filter(flowElement -> flowElement.getClass().equals(StartEvent.class))
                .findFirst();

            if (startEvent.isPresent()) {
                return new ExtensionsStartEventId(
                    startEvent.get().getId(),
                    cachingProcessExtensionService.getExtensionsForId(id)
                );
            }
        }

        return null;
    }

    private record ExtensionsStartEventId(String id, Extension extensions) {}
}
