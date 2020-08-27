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
package org.activiti.cloud.services.query.rest.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.rest.ServiceTaskController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ServiceTaskRepresentationModelAssembler implements RepresentationModelAssembler<BPMNActivityEntity, EntityModel<CloudBPMNActivity>> {

    @Override
    public EntityModel<CloudBPMNActivity> toModel(BPMNActivityEntity entity) {
        Link selfRel = linkTo(methodOn(ServiceTaskController.class).findById(entity.getId())).withSelfRel();

        return new EntityModel<>(entity,
                                 selfRel);
    }

}
