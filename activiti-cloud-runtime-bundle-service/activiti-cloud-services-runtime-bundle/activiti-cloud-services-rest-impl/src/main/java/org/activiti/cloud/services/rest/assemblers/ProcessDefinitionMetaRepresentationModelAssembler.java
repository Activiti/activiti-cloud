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
package org.activiti.cloud.services.rest.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.services.api.model.ProcessDefinitionMeta;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionMetaControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ProcessDefinitionMetaRepresentationModelAssembler
    implements RepresentationModelAssembler<ProcessDefinitionMeta, EntityModel<ProcessDefinitionMeta>> {

    @Override
    public EntityModel<ProcessDefinitionMeta> toModel(ProcessDefinitionMeta processDefinitionMeta) {
        Link metadata = linkTo(
            methodOn(ProcessDefinitionMetaControllerImpl.class)
                .getProcessDefinitionMetadata(processDefinitionMeta.getId())
        )
            .withRel("meta");
        Link selfRel = linkTo(
            methodOn(ProcessDefinitionControllerImpl.class).getProcessDefinition(processDefinitionMeta.getId())
        )
            .withSelfRel();
        Link startProcessLink = linkTo(methodOn(ProcessInstanceControllerImpl.class).startProcess(null))
            .withRel("startProcess");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");

        return EntityModel.of(processDefinitionMeta, metadata, selfRel, startProcessLink, homeLink);
    }
}
