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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.controllers.AuditEventsAdminController;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL,
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class AuditEventsAdminControllerImpl implements AuditEventsAdminController {

    private final EventsRepository eventsRepository;

    private final EventRepresentationModelAssembler eventRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler;

    private final APIEventToEntityConverters eventConverters;

    private final AuditEventsExporter auditEventsExporter;

    private final AuditEventsAdminService auditEventsAdminService;

    @Autowired
    public AuditEventsAdminControllerImpl(
        EventsRepository eventsRepository,
        EventRepresentationModelAssembler eventRepresentationModelAssembler,
        APIEventToEntityConverters eventConverters,
        AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler,
        ObjectMapper objectMapper,
        AuditEventsAdminService auditEventsAdminService
    ) {
        this.eventsRepository = eventsRepository;
        this.eventRepresentationModelAssembler = eventRepresentationModelAssembler;
        this.eventConverters = eventConverters;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.auditEventsExporter = new AuditEventsExporter(objectMapper);
        this.auditEventsAdminService = auditEventsAdminService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> findAll(Pageable pageable) {
        Page<AuditEventEntity> allAuditInPage = eventsRepository.findAll(pageable);

        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = toCloudRuntimeEvents(allAuditInPage.getContent());

        return pagedCollectionModelAssembler.toModel(
            pageable,
            new PageImpl<>(events, pageable, allAuditInPage.getTotalElements()),
            eventRepresentationModelAssembler
        );
    }

    @GetMapping(path = "/export/{fileName}")
    public void export(
        @PathVariable(value = "fileName") String fileName,
        @RequestParam(value = "from", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        HttpServletResponse response
    ) throws Exception {
        Collection<AuditEventEntity> audits = auditEventsAdminService.findAuditsBetweenDates(from, to);

        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = toCloudRuntimeEvents(audits);

        auditEventsExporter.exportCsv(events, fileName, response);
    }

    private List<CloudRuntimeEvent<?, CloudRuntimeEventType>> toCloudRuntimeEvents(
        Iterable<AuditEventEntity> allAuditInPage
    ) {
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = new ArrayList<>();

        for (AuditEventEntity aee : allAuditInPage) {
            events.add(eventConverters.getConverterByEventTypeName(aee.getEventType()).convertToAPI(aee));
        }
        return events;
    }
}
