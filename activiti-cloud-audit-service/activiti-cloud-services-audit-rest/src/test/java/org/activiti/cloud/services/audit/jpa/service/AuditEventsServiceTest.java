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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.api.search.SearchParams;
import org.activiti.cloud.services.audit.jpa.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery;
import org.springframework.hateoas.EntityModel;

@ExtendWith(MockitoExtension.class)
class AuditEventsServiceTest {

    @Mock
    private EventsRepository<AuditEventEntity> eventsRepository;

    @Mock
    private EventRepresentationModelAssembler eventRepresentationModelAssembler;

    @Mock
    private APIEventToEntityConverters eventConverters;

    @Mock
    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    @Mock
    private AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler;

    @InjectMocks
    private AuditEventsService auditEventsService;

    private final SearchParams emptySearch = new SearchParams(null, null, null);

    @Test
    void should_throwNotFound_when_eventIdDoesNotExist() {
        given(eventsRepository.findByEventId("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditEventsService.findEventById("missing"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Unable to find event for the given id:'missing'");
    }

    @Test
    void should_throwForbidden_when_userCannotReadEvent() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getProcessDefinitionId()).willReturn("procDef");
        given(eventsRepository.findByEventId("id")).willReturn(Optional.of(entity));
        given(securityPoliciesApplicationService.canRead(any(), any())).willReturn(false);

        assertThatThrownBy(() -> auditEventsService.findEventById("id"))
            .isInstanceOf(ActivitiForbiddenException.class)
            .hasMessage("Operation not permitted for procDef");
    }

    @Test
    void should_returnModel_when_eventIsReadable() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getEventType()).willReturn("PROCESS_STARTED");
        given(eventsRepository.findByEventId("id")).willReturn(Optional.of(entity));
        given(securityPoliciesApplicationService.canRead(any(), any())).willReturn(true);
        EventToEntityConverter converter = converterReturning(entity);
        given(eventConverters.getConverterByEventTypeName("PROCESS_STARTED")).willReturn(converter);

        EntityModel<CloudRuntimeEvent<?, CloudRuntimeEventType>> expected = EntityModel.of(cloudRuntimeEvent());
        given(eventRepresentationModelAssembler.toModel(any())).willReturn(expected);

        assertThat(auditEventsService.findEventById("id")).isSameAs(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_useExactCount_when_searchingV1() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getEventType()).willReturn("PROCESS_STARTED");
        Pageable pageable = PageRequest.of(0, 20);
        given(securityPoliciesApplicationService.createSpecWithSecurity(any(), any())).willReturn(
            mock(Specification.class)
        );
        given(eventsRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(entity), pageable, 57)
        );
        EventToEntityConverter converter = converterReturning(entity);
        given(eventConverters.getConverterByEventTypeName("PROCESS_STARTED")).willReturn(converter);

        auditEventsService.searchEvents(emptySearch, pageable);

        ArgumentCaptor<Page<CloudRuntimeEvent<?, CloudRuntimeEventType>>> pageCaptor = ArgumentCaptor.forClass(
            Page.class
        );
        verify(pagedCollectionModelAssembler).toModel(any(Pageable.class), pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getTotalElements()).isEqualTo(57);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_computeKnownElements_when_searchingV2Sliced() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getEventType()).willReturn("PROCESS_STARTED");
        Pageable pageable = PageRequest.of(2, 2);
        given(securityPoliciesApplicationService.createSpecWithSecurity(any(), any())).willReturn(
            mock(Specification.class)
        );
        givenSlice(new SliceImpl<>(List.of(entity, entity), pageable, true));
        EventToEntityConverter converter = converterReturning(entity);
        given(eventConverters.getConverterByEventTypeName("PROCESS_STARTED")).willReturn(converter);

        auditEventsService.searchEventsSliced(emptySearch, pageable);

        ArgumentCaptor<Page<CloudRuntimeEvent<?, CloudRuntimeEventType>>> pageCaptor = ArgumentCaptor.forClass(
            Page.class
        );
        verify(pagedCollectionModelAssembler).toModel(any(Pageable.class), pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getTotalElements()).isEqualTo(pageable.getOffset() + 2 + 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_defaultSortToTimestampDesc_when_unsorted() {
        Pageable pageable = PageRequest.of(0, 20);
        given(securityPoliciesApplicationService.createSpecWithSecurity(any(), any())).willReturn(
            mock(Specification.class)
        );
        given(eventsRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(), pageable, 0)
        );

        auditEventsService.searchEvents(emptySearch, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventsRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).containsExactly(
            new Sort.Order(Sort.Direction.DESC, "timestamp")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_skipEvent_when_converterNotFound() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getEventType()).willReturn("UNKNOWN_TYPE");
        Pageable pageable = PageRequest.of(0, 20);
        given(securityPoliciesApplicationService.createSpecWithSecurity(any(), any())).willReturn(
            mock(Specification.class)
        );
        given(eventsRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(
            new PageImpl<>(List.of(entity), pageable, 1)
        );
        given(eventConverters.getConverterByEventTypeName("UNKNOWN_TYPE")).willReturn(null);

        auditEventsService.searchEvents(emptySearch, pageable);

        ArgumentCaptor<Page<CloudRuntimeEvent<?, CloudRuntimeEventType>>> pageCaptor = ArgumentCaptor.forClass(
            Page.class
        );
        verify(pagedCollectionModelAssembler).toModel(any(Pageable.class), pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getContent()).isEmpty();
    }

    private CloudRuntimeEvent<?, CloudRuntimeEventType> cloudRuntimeEvent() {
        return mock(CloudRuntimeEvent.class);
    }

    private EventToEntityConverter converterReturning(AuditEventEntity entity) {
        EventToEntityConverter converter = mock(EventToEntityConverter.class);
        given(converter.convertToAPI(entity)).willReturn(cloudRuntimeEvent());
        return converter;
    }

    @SuppressWarnings("unchecked")
    private void givenSlice(Slice<AuditEventEntity> slice) {
        SpecificationFluentQuery<AuditEventEntity> fluentQuery = mock(SpecificationFluentQuery.class);
        given(fluentQuery.slice(any(Pageable.class))).willReturn(slice);
        given(eventsRepository.findBy(any(Specification.class), any())).willAnswer(invocation -> {
            Function<SpecificationFluentQuery<AuditEventEntity>, Object> queryFunction = invocation.getArgument(1);
            return queryFunction.apply(fluentQuery);
        });
    }
}
