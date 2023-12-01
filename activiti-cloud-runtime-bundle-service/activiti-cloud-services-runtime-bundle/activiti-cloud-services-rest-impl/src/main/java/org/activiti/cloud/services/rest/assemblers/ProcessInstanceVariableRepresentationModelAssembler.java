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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ProcessInstanceVariableRepresentationModelAssembler
    implements RepresentationModelAssembler<VariableInstance, EntityModel<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public ProcessInstanceVariableRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public EntityModel<CloudVariableInstance> toModel(VariableInstance variableInstance) {
        CloudVariableInstance cloudVariableInstance = converter.from(variableInstance);
        Link processVariables = linkTo(
            methodOn(ProcessInstanceVariableControllerImpl.class)
                .getVariables(cloudVariableInstance.getProcessInstanceId())
        )
            .withRel("processVariables");
        Link processInstance = linkTo(
            methodOn(ProcessInstanceControllerImpl.class)
                .getProcessInstanceById(cloudVariableInstance.getProcessInstanceId())
        )
            .withRel("processInstance");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return EntityModel.of(cloudVariableInstance, processVariables, processInstance, homeLink);
    }
}
