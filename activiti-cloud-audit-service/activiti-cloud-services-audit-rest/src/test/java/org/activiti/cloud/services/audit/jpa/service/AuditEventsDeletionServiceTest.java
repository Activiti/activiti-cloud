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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatus;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatusResponse;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AuditEventsDeletionServiceTest {

    @Mock
    private EventsRepository<AuditEventEntity> eventsRepository;

    @Test
    void should_delete_events_in_batches_and_complete() {
        AuditEventsDeletionService auditEventsDeletionService = new AuditEventsDeletionService(eventsRepository, 2);
        PageRequest pageRequest = PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("id").ascending());

        AuditEventEntity firstEvent = new ProcessStartedAuditEventEntity();
        firstEvent.setId(1L);
        AuditEventEntity secondEvent = new ProcessStartedAuditEventEntity();
        secondEvent.setId(2L);

        when(eventsRepository.count()).thenReturn(2L);
        when(eventsRepository.findAll(pageRequest))
            .thenReturn(new PageImpl<>(List.of(firstEvent, secondEvent), PageRequest.of(0, 2), 2))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 2), 0));

        assertThat(auditEventsDeletionService.startDeletion()).isTrue();

        AuditEventsDeletionStatusResponse response = auditEventsDeletionService.deleteEventsAsync().join();

        assertThat(response.getStatus()).isEqualTo(AuditEventsDeletionStatus.COMPLETED);
        assertThat(response.getDeletedCount()).isEqualTo(2);
        assertThat(response.getRemainingCount()).isZero();
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getPercentComplete()).isEqualTo(100.0);
        verify(eventsRepository).deleteById(1L);
        verify(eventsRepository).deleteById(2L);
    }

    @Test
    void should_cancel_deletion_after_requesting_cancellation() {
        AuditEventsDeletionService auditEventsDeletionService = new AuditEventsDeletionService(eventsRepository, 2);
        PageRequest pageRequest = PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("id").ascending());

        AuditEventEntity firstEvent = new ProcessStartedAuditEventEntity();
        firstEvent.setId(1L);
        AuditEventEntity secondEvent = new ProcessStartedAuditEventEntity();
        secondEvent.setId(2L);

        when(eventsRepository.count()).thenReturn(2L);
        when(eventsRepository.findAll(pageRequest))
            .thenReturn(new PageImpl<>(List.of(firstEvent, secondEvent), PageRequest.of(0, 2), 2));
        doAnswer(invocation -> {
            auditEventsDeletionService.requestCancellation();
            return null;
        })
            .when(eventsRepository)
            .deleteById(1L);

        assertThat(auditEventsDeletionService.startDeletion()).isTrue();

        AuditEventsDeletionStatusResponse response = auditEventsDeletionService.deleteEventsAsync().join();

        assertThat(response.getStatus()).isEqualTo(AuditEventsDeletionStatus.CANCELLED);
        assertThat(response.getDeletedCount()).isEqualTo(1);
        assertThat(response.getRemainingCount()).isEqualTo(1);
        assertThat(response.getTotalCount()).isEqualTo(2);
    }

    @Test
    void should_reject_start_when_deletion_is_already_running() {
        AuditEventsDeletionService auditEventsDeletionService = new AuditEventsDeletionService(eventsRepository, 2);

        when(eventsRepository.count()).thenReturn(1L);

        assertThat(auditEventsDeletionService.startDeletion()).isTrue();
        assertThat(auditEventsDeletionService.startDeletion()).isFalse();
    }
}
