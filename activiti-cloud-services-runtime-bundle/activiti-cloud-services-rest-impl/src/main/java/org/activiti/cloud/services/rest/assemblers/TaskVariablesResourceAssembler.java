/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.api.model.TaskVariables;
import org.activiti.cloud.services.rest.api.resources.VariablesResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class TaskVariablesResourceAssembler extends ResourceAssemblerSupport<TaskVariables, VariablesResource> {

    public TaskVariablesResourceAssembler() {
        super(TaskVariableControllerImpl.class,
              VariablesResource.class);
    }

    @Override
    public VariablesResource toResource(TaskVariables taskVariables) {
        Link selfRel;
        if (TaskVariables.TaskVariableScope.GLOBAL.equals(taskVariables.getScope())) {
            selfRel = linkTo(methodOn(TaskVariableControllerImpl.class).getVariables(taskVariables.getTaskId())).withSelfRel();
        } else {
            selfRel = linkTo(methodOn(TaskVariableControllerImpl.class).getVariablesLocal(taskVariables.getTaskId())).withSelfRel();
        }
        Link taskRel = linkTo(methodOn(TaskControllerImpl.class).getTaskById(taskVariables.getTaskId())).withRel("task");
        Link homeLink = linkTo(HomeControllerImpl.class).withRel("home");
        return new VariablesResource(taskVariables.getVariables(),
                                     selfRel,
                                     taskRel,
                                     homeLink);
    }
}
