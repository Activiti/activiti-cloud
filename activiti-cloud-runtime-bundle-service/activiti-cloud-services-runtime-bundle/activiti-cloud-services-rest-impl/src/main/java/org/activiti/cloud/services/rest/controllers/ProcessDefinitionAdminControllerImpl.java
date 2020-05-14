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

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessDefinitionAdminController;
import org.activiti.cloud.services.rest.assemblers.ProcessDefinitionResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessDefinitionAdminControllerImpl implements ProcessDefinitionAdminController {


    private final ProcessDefinitionResourceAssembler resourceAssembler;

    private final ProcessAdminRuntime processAdminRuntime;

    private final AlfrescoPagedResourcesAssembler<ProcessDefinition> pagedResourcesAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessDefinitionAdminControllerImpl(ProcessAdminRuntime processAdminRuntime,
                                                ProcessDefinitionResourceAssembler resourceAssembler,
                                                AlfrescoPagedResourcesAssembler<ProcessDefinition> pagedResourcesAssembler,
                                                SpringPageConverter pageConverter) {
        this.processAdminRuntime = processAdminRuntime;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<Resource<CloudProcessDefinition>> getAllProcessDefinitions(Pageable pageable) {
        Page<ProcessDefinition> page = processAdminRuntime.processDefinitions(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                pageConverter.toSpringPage(pageable, page),
                resourceAssembler);
    }

}
