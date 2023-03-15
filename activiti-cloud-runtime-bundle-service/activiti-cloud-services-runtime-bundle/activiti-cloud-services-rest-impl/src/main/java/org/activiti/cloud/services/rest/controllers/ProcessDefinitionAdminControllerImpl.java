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

import java.util.List;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.cloud.services.core.ProcessDefinitionAdminService;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessDefinitionAdminController;
import org.activiti.cloud.services.rest.assemblers.ExtendedCloudProcessDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessDefinitionAdminControllerImpl implements ProcessDefinitionAdminController {

    private final ProcessDefinitionRepresentationModelAssembler representationModelAssembler;

    private final ExtendedCloudProcessDefinitionRepresentationModelAssembler extendedCloudProcessDefinitionRepresentationModelAssembler;

    private final ProcessDefinitionAdminService processDefinitionAdminService;

    private final AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessDefinitionAdminControllerImpl(
        ProcessDefinitionRepresentationModelAssembler representationModelAssembler,
        ExtendedCloudProcessDefinitionRepresentationModelAssembler extendedCloudProcessDefinitionRepresentationModelAssembler,
        AlfrescoPagedModelAssembler<ProcessDefinition> pagedCollectionModelAssembler,
        SpringPageConverter pageConverter,
        ProcessDefinitionAdminService processDefinitionAdminService
    ) {
        this.representationModelAssembler = representationModelAssembler;
        this.extendedCloudProcessDefinitionRepresentationModelAssembler =
            extendedCloudProcessDefinitionRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
        this.processDefinitionAdminService = processDefinitionAdminService;
    }

    @Override
    public PagedModel<EntityModel<ExtendedCloudProcessDefinition>> getAllProcessDefinitions(
        @RequestParam(required = false, defaultValue = "") List<String> include,
        Pageable pageable
    ) {
        Page<ProcessDefinition> page = processDefinitionAdminService.getProcessDefinitions(
            pageConverter.toAPIPageable(pageable),
            include
        );
        return pagedCollectionModelAssembler.toModel(
            pageable,
            pageConverter.toSpringPage(pageable, page),
            extendedCloudProcessDefinitionRepresentationModelAssembler
        );
    }
}
