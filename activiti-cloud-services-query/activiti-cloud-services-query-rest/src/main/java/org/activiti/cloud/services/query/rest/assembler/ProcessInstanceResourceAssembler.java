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

package org.activiti.cloud.services.query.rest.assembler;

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.resources.ProcessInstanceResource;
import org.activiti.cloud.services.query.rest.ProcessInstanceController;
import org.activiti.cloud.services.query.rest.ProcessInstanceTasksController;
import org.activiti.cloud.services.query.rest.ProcessInstanceVariableController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class ProcessInstanceResourceAssembler implements ResourceAssembler<ProcessInstanceEntity, ProcessInstanceResource> {

    @Override
    public ProcessInstanceResource toResource(ProcessInstanceEntity entity) {
        Link selfRel = linkTo(methodOn(ProcessInstanceController.class).findById(entity.getId())).withSelfRel();
        Link tasksRel = linkTo(methodOn(ProcessInstanceTasksController.class).getTasks(entity.getId(), null)).withRel("tasks");
        Link variablesRel = linkTo(methodOn(ProcessInstanceVariableController.class).getVariables(entity.getId(),null,null)).withRel("variables");
        return new ProcessInstanceResource(entity, selfRel, tasksRel, variablesRel);
    }

}