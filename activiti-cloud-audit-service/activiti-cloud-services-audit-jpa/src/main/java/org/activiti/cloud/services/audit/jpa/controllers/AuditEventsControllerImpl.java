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

package org.activiti.cloud.services.audit.jpa.controllers;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.api.controllers.AuditEventsController;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventSpecificationsBuilder;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.repository.SearchOperation;
import org.activiti.cloud.services.audit.jpa.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping(value = "/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class AuditEventsControllerImpl implements AuditEventsController {

    private static Logger LOGGER = LoggerFactory.getLogger(AuditEventsAdminControllerImpl.class);

    private final EventsRepository eventsRepository;

    private final EventRepresentationModelAssembler eventRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler;

    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    private final APIEventToEntityConverters eventConverters;

    @Autowired
    public AuditEventsControllerImpl(EventsRepository eventsRepository,
                                     EventRepresentationModelAssembler eventRepresentationModelAssembler,
                                     APIEventToEntityConverters eventConverters,
                                     SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService,
                                     AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler) {
        this.eventsRepository = eventsRepository;
        this.eventRepresentationModelAssembler = eventRepresentationModelAssembler;
        this.eventConverters = eventConverters;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
    public EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>> findById(@PathVariable String eventId) {
        Optional<AuditEventEntity> findResult = eventsRepository.findByEventId(eventId);
        if (!findResult.isPresent()) {
            throw new NotFoundException("Unable to find event for the given id:'" + eventId + "'");
        }
        AuditEventEntity auditEventEntity = findResult.get();
        if (!securityPoliciesApplicationService.canRead(auditEventEntity.getProcessDefinitionId(),
                                                        auditEventEntity.getServiceFullName())) {
            throw new ActivitiForbiddenException("Operation not permitted for " + auditEventEntity.getProcessDefinitionId());
        }

        CloudRuntimeEvent cloudRuntimeEvent = eventConverters.getConverterByEventTypeName(auditEventEntity.getEventType()).convertToAPI(auditEventEntity);
        return eventRepresentationModelAssembler.toModel(cloudRuntimeEvent);
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> findAll(@RequestParam(value = "search", required = false) String search,
                                                 Pageable pageable) {

        Specification<AuditEventEntity> spec = createSearchSpec(search);

        spec = securityPoliciesApplicationService.createSpecWithSecurity(spec,
                                                                         SecurityPolicyAccess.READ);

        Page<AuditEventEntity> allAuditInPage = eventsRepository.findAll(spec,
                                                                         pageable);
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = new ArrayList<>();

        for (AuditEventEntity aee : allAuditInPage.getContent()) {
            EventToEntityConverter converterByEventTypeName = eventConverters.getConverterByEventTypeName(aee.getEventType());
            if (converterByEventTypeName != null) {
                events.add(converterByEventTypeName.convertToAPI(aee));
            } else {
                LOGGER.warn("Converter not found for Event Type: " + aee.getEventType());
            }
        }

        return pagedCollectionModelAssembler.toModel(pageable,
                                                  new PageImpl<>(events,
                                                                 pageable,
                                                                 allAuditInPage.getTotalElements()),
                                                  eventRepresentationModelAssembler);
    }

    private Specification<AuditEventEntity> createSearchSpec(String search) {
        EventSpecificationsBuilder builder = new EventSpecificationsBuilder();
        if (search != null && !search.isEmpty()) {
            String operationSetExpr = Arrays.asList(SearchOperation.SIMPLE_OPERATION_SET).stream().collect(joining("|"));
            Pattern pattern = Pattern.compile("(\\w+?)(" + operationSetExpr + ")(\\p{Punct}?)([a-zA-Z0-9-_]+?)(\\p{Punct}?),");
            Matcher matcher = pattern.matcher(search + ",");
            while (matcher.find()) {
                builder.with(matcher.group(1),
                             matcher.group(2),
                             matcher.group(4),
                             matcher.group(3),
                             matcher.group(5));
            }
        }

        return builder.build();
    }
}
