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
package org.activiti.cloud.services.audit.api.assembler;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.controllers.AuditEventsController;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import java.lang.reflect.Method;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class EventRepresentationModelAssembler implements RepresentationModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>, EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> {

    @Override
    public EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>> toModel(CloudRuntimeEvent<?, CloudRuntimeEventType> event) {
        Link selfRel = linkTo(getFindByIdMethod(), event.getId()).withSelfRel();
        return new EntityModel<>(event,
                                 selfRel);
    }

    private Method getFindByIdMethod() {
        try {
            return AuditEventsController.class.getMethod("findById", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
