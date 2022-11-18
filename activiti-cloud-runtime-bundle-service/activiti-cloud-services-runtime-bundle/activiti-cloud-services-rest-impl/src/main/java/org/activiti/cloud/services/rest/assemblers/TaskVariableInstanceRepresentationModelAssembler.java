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
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class TaskVariableInstanceRepresentationModelAssembler
    implements RepresentationModelAssembler<VariableInstance, EntityModel<CloudVariableInstance>> {

    private ToCloudVariableInstanceConverter converter;

    public TaskVariableInstanceRepresentationModelAssembler(ToCloudVariableInstanceConverter converter) {
        this.converter = converter;
    }

    @Override
    public EntityModel<CloudVariableInstance> toModel(VariableInstance taskVariable) {
        CloudVariableInstance cloudVariableInstance = converter.from(taskVariable);
        Link globalVariables = linkTo(
            methodOn(TaskVariableControllerImpl.class).getVariables(cloudVariableInstance.getTaskId())
        )
            .withRel("variables");
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudVariableInstance.getTaskId()))
            .withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return EntityModel.of(cloudVariableInstance, globalVariables, taskRel, homeLink);
    }
}
