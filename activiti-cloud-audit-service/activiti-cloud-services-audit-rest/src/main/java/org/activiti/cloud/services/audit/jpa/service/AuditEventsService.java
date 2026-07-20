/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.audit.jpa.service;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.search.SearchParams;
import org.activiti.cloud.services.audit.jpa.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventSpecificationsBuilder;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.repository.SearchOperation;
import org.activiti.cloud.services.audit.jpa.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

public class AuditEventsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventsService.class);

    private final EventsRepository<AuditEventEntity> eventsRepository;

    private final EventRepresentationModelAssembler eventRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<
        CloudRuntimeEvent<?, CloudRuntimeEventType>
    > pagedCollectionModelAssembler;

    private final SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    private final APIEventToEntityConverters eventConverters;

    public AuditEventsService(
        EventsRepository<AuditEventEntity> eventsRepository,
        EventRepresentationModelAssembler eventRepresentationModelAssembler,
        APIEventToEntityConverters eventConverters,
        SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService,
        AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler
    ) {
        this.eventsRepository = eventsRepository;
        this.eventRepresentationModelAssembler = eventRepresentationModelAssembler;
        this.eventConverters = eventConverters;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
    }

    public EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>> findEventById(String eventId) {
        Optional<AuditEventEntity> findResult = eventsRepository.findByEventId(eventId);
        if (!findResult.isPresent()) {
            throw new NotFoundException("Unable to find event for the given id:'" + eventId + "'");
        }
        AuditEventEntity auditEventEntity = findResult.get();
        if (
            !securityPoliciesApplicationService.canRead(
                auditEventEntity.getProcessDefinitionId(),
                auditEventEntity.getServiceFullName()
            )
        ) {
            throw new ActivitiForbiddenException(
                "Operation not permitted for " + auditEventEntity.getProcessDefinitionId()
            );
        }

        CloudRuntimeEvent cloudRuntimeEvent = eventConverters
            .getConverterByEventTypeName(auditEventEntity.getEventType())
            .convertToAPI(auditEventEntity);
        return eventRepresentationModelAssembler.toModel(cloudRuntimeEvent);
    }

    public PagedModel<EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> searchEvents(
        SearchParams searchParams,
        Pageable pageable
    ) {
        pageable = applyDefaultSort(pageable);

        Specification<AuditEventEntity> spec = securedSearchSpec(searchParams);

        Page<AuditEventEntity> allAuditInPage = eventsRepository.findAll(spec, pageable);
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = toCloudRuntimeEvents(allAuditInPage.getContent());

        return pagedCollectionModelAssembler.toModel(
            pageable,
            new PageImpl<>(events, pageable, allAuditInPage.getTotalElements()),
            eventRepresentationModelAssembler
        );
    }

    public PagedModel<EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>>> searchEventsSliced(
        SearchParams searchParams,
        Pageable pageable
    ) {
        pageable = applyDefaultSort(pageable);

        Specification<AuditEventEntity> spec = securedSearchSpec(searchParams);

        Pageable slicePageable = pageable;
        Slice<AuditEventEntity> allAuditInPage = eventsRepository.findBy(spec, query -> query.slice(slicePageable));
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = toCloudRuntimeEvents(allAuditInPage.getContent());

        long knownElements = pageable.getOffset() + events.size() + (allAuditInPage.hasNext() ? 1 : 0);

        return pagedCollectionModelAssembler.toModel(
            pageable,
            new PageImpl<>(events, pageable, knownElements),
            eventRepresentationModelAssembler
        );
    }

    private Pageable applyDefaultSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "timestamp");
            if (pageable instanceof AlfrescoPageRequest alfrescoPageRequest) {
                Pageable inner = alfrescoPageRequest.getPageable();
                return new AlfrescoPageRequest(
                    alfrescoPageRequest.getOffset(),
                    alfrescoPageRequest.getPageSize(),
                    PageRequest.of(inner.getPageNumber(), inner.getPageSize(), defaultSort)
                );
            }
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        return pageable;
    }

    private Specification<AuditEventEntity> securedSearchSpec(SearchParams searchParams) {
        Specification<AuditEventEntity> spec = createSearchSpec(searchParams);
        return securityPoliciesApplicationService.createSpecWithSecurity(spec, SecurityPolicyAccess.READ);
    }

    private List<CloudRuntimeEvent<?, CloudRuntimeEventType>> toCloudRuntimeEvents(
        Iterable<? extends AuditEventEntity> auditEntities
    ) {
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = new ArrayList<>();
        for (AuditEventEntity aee : auditEntities) {
            EventToEntityConverter converterByEventTypeName = eventConverters.getConverterByEventTypeName(
                aee.getEventType()
            );
            if (converterByEventTypeName != null) {
                events.add(converterByEventTypeName.convertToAPI(aee));
            } else {
                LOGGER.warn("Converter not found for Event Type: " + aee.getEventType());
            }
        }
        return events;
    }

    private Specification<AuditEventEntity> createSearchSpec(SearchParams searchParams) {
        EventSpecificationsBuilder builder = new EventSpecificationsBuilder();
        String search = searchParams.search();
        if (search != null && !search.isEmpty()) {
            String operationSetExpr = Arrays.asList(SearchOperation.SIMPLE_OPERATION_SET)
                .stream()
                .collect(joining("|"));
            Pattern pattern = Pattern.compile(
                "(\\w+?)(" + operationSetExpr + ")(\\p{Punct}?)([a-zA-Z0-9-_]+?)(\\p{Punct}?),"
            );
            Matcher matcher = pattern.matcher(search + ",");
            while (matcher.find()) {
                builder.with(matcher.group(1), matcher.group(2), matcher.group(4), matcher.group(3), matcher.group(5));
            }
        }
        if (searchParams.eventTimeFrom() != null) {
            builder.with("timestamp", ">=", searchParams.eventTimeFrom().getTime(), null, null);
        }
        if (searchParams.eventTimeTo() != null) {
            builder.with("timestamp", "<=", searchParams.eventTimeTo().getTime(), null, null);
        }
        return builder.build();
    }
}
