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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery;

@ExtendWith(MockitoExtension.class)
class AuditEventsAdminServiceTest {

    @Mock
    private EventsRepository<AuditEventEntity> eventsRepository;

    @Mock
    private APIEventToEntityConverters eventConverters;

    @Mock
    private EventRepresentationModelAssembler eventRepresentationModelAssembler;

    @Mock
    private AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler;

    @InjectMocks
    private AuditEventsAdminService auditEventsAdminService;

    @Test
    void should_throw_exception_when_from_date_is_after_to_date() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2019, 1, 1);

        // when
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate)
        );

        // then
        assertEquals("From date cannot be after to date", thrown.getMessage());
    }

    @Test
    void should_throw_exception_when_difference_between_dates_is_more_than_31_days() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2020, 3, 1);

        // when
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate)
        );

        // then
        assertEquals("Difference between dates cannot be more than 31 days", thrown.getMessage());
    }

    @Test
    void should_return_events_between_dates() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2020, 1, 2);

        // when
        auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate);

        // then
        verify(eventsRepository).findAllByTimestampBetweenOrderByTimestampDesc(anyLong(), anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_useSliceAndComputeKnownElements_when_findingAllSliced() {
        AuditEventEntity entity = mock(AuditEventEntity.class);
        given(entity.getEventType()).willReturn("PROCESS_STARTED");
        Pageable pageable = PageRequest.of(2, 2);
        givenSlice(new SliceImpl<>(List.of(entity, entity), pageable, true));
        EventToEntityConverter converter = mock(EventToEntityConverter.class);
        given(converter.convertToAPI(entity)).willReturn(mock(CloudRuntimeEvent.class));
        given(eventConverters.getConverterByEventTypeName("PROCESS_STARTED")).willReturn(converter);

        auditEventsAdminService.findAllSliced(pageable);

        ArgumentCaptor<Page<CloudRuntimeEvent<?, CloudRuntimeEventType>>> pageCaptor = ArgumentCaptor.forClass(
            Page.class
        );
        verify(pagedCollectionModelAssembler).toModel(any(Pageable.class), pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getTotalElements()).isEqualTo(pageable.getOffset() + 2 + 1);
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
