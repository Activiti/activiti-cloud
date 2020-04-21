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

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessDefinitionAdminController;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessDefinitionAdminControllerImpl implements ProcessDefinitionAdminController {


    private final ProcessDefinitionRepresentationModelAssembler representationModelAssembler;

    private final ProcessAdminRuntime processAdminRuntime;

    private final AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessDefinitionAdminControllerImpl(ProcessAdminRuntime processAdminRuntime,
                                                ProcessDefinitionRepresentationModelAssembler representationModelAssembler,
                                                AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler,
                                                SpringPageConverter pageConverter) {
        this.processAdminRuntime = processAdminRuntime;
        this.representationModelAssembler = representationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedModel<EntityModel<CloudProcessDefinition>> getAllProcessDefinitions(Pageable pageable) {
        Page<ProcessDefinition> page = processAdminRuntime.processDefinitions(pageConverter.toAPIPageable(pageable));
        return pagedCollectionModelAssembler.toModel(pageable,
                pageConverter.toSpringPage(pageable, page),
                representationModelAssembler);
    }

}
