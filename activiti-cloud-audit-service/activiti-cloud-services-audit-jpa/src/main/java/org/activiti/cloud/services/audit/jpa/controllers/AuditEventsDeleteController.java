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
package org.activiti.cloud.services.audit.jpa.controllers;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.assembler.EventResourceAssembler;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.resources.EventsRelProvider;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
        value = "/admin/v1/" + EventsRelProvider.COLLECTION_RESOURCE_REL,
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class AuditEventsDeleteController {

    private final EventsRepository eventsRepository;

    private final EventResourceAssembler eventResourceAssembler;

    private final APIEventToEntityConverters eventConverters;

    @Autowired
    public AuditEventsDeleteController(EventsRepository eventsRepository,
                                       EventResourceAssembler eventResourceAssembler,
                                       APIEventToEntityConverters eventConverters) {
        this.eventsRepository = eventsRepository;
        this.eventResourceAssembler = eventResourceAssembler;
        this.eventConverters = eventConverters;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Resources<Resource<CloudRuntimeEvent<?, CloudRuntimeEventType>>> deleteEvents (){

        Collection<Resource<CloudRuntimeEvent<?, CloudRuntimeEventType>>> result = new ArrayList<>();
        Iterable <AuditEventEntity> iterable = eventsRepository.findAll();

        for(AuditEventEntity entity : iterable){
            result.add(eventResourceAssembler.toResource(
                    eventConverters.getConverterByEventTypeName(entity.getEventType()).convertToAPI(entity)
            ));
        }

        eventsRepository.deleteAll(iterable);

        return new Resources<>(result);
    }


}
