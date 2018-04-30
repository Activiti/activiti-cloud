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

package org.activiti.cloud.services.audit.mongo;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.audit.mongo.assembler.EventResourceAssembler;
import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.activiti.cloud.services.audit.mongo.repository.EventsRepository;
import org.activiti.cloud.services.audit.mongo.resources.EventResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/" + EventsRelProvider.COLLECTION_RESOURCE_REL, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class ProcessEngineEventsAdminController {

    private final EventsRepository eventsRepository;

    private EventResourceAssembler eventResourceAssembler;

    private AlfrescoPagedResourcesAssembler<ProcessEngineEventDocument> pagedResourcesAssembler;

    @Autowired
    public ProcessEngineEventsAdminController(EventsRepository eventsRepository,
                                              EventResourceAssembler eventResourceAssembler,
                                              AlfrescoPagedResourcesAssembler<ProcessEngineEventDocument> pagedResourcesAssembler) {
        this.eventsRepository = eventsRepository;
        this.eventResourceAssembler = eventResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<EventResource> findAll(@QuerydslPredicate(root = ProcessEngineEventDocument.class) Predicate predicate,
                                                 Pageable pageable) {


        return pagedResourcesAssembler.toResource(pageable,eventsRepository.findAll(predicate,
                pageable),
                eventResourceAssembler);
    }
}