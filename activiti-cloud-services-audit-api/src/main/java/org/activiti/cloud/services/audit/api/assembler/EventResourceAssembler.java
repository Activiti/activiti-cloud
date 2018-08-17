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

package org.activiti.cloud.services.audit.api.assembler;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.controllers.AuditEventsController;
import org.activiti.cloud.services.audit.api.resources.EventResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class EventResourceAssembler extends ResourceAssemblerSupport<CloudRuntimeEvent, EventResource> {

    @Autowired
    public EventResourceAssembler() {
        super(AuditEventsController.class,
              EventResource.class);
    }

    @Override
    public EventResource toResource(CloudRuntimeEvent event) {
        Link selfRel = linkTo(methodOn(AuditEventsController.class).findById(event.getId())).withSelfRel();
        return new EventResource(event,
                                 selfRel);
    }
}