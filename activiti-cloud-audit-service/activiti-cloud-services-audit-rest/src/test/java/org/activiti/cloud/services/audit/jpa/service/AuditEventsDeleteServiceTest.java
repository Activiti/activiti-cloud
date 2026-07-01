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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeleteService.DeleteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AuditEventsDeleteServiceTest {

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private AuditEventsDeleteService deleteService;

    @BeforeEach
    void setUp() {
        deleteService = new AuditEventsDeleteService(eventsRepository, transactionTemplate, 100);
    }

    @Test
    void shouldStartDeletionAndComplete() {
        when(eventsRepository.count()).thenReturn(1L);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Page<AuditEventEntity> page = new PageImpl<>(List.of(buildEvent(1L)));
        when(eventsRepository.findAll(any(Pageable.class))).thenReturn(page);

        deleteService.startDeletion();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(deleteService.getStatus()).isEqualTo(DeleteStatus.COMPLETED);
            assertThat(deleteService.getDeletedCount()).isEqualTo(1L);
        });
    }

    @Test
    void shouldStopDeletionWhenRequested() {
        when(eventsRepository.count()).thenReturn(1000L);

        AtomicInteger callCount = new AtomicInteger(0);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callCount.incrementAndGet();
            Thread.sleep(50);
            return callback.doInTransaction(null);
        });

        List<AuditEventEntity> events = List.of(buildEvent(1L), buildEvent(2L));
        Page<AuditEventEntity> page = new PageImpl<>(events, Pageable.ofSize(100), 1000);
        when(eventsRepository.findAll(any(Pageable.class))).thenReturn(page);

        deleteService.startDeletion();

        await().atMost(Duration.ofSeconds(2)).until(() -> deleteService.getStatus() == DeleteStatus.RUNNING);

        deleteService.stopDeletion();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(deleteService.getStatus()).isEqualTo(DeleteStatus.STOPPED)
        );
    }

    @Test
    void shouldThrowWhenStartingWhileAlreadyRunning() {
        when(eventsRepository.count()).thenReturn(1000L);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            Thread.sleep(500);
            return true;
        });

        deleteService.startDeletion();

        await().atMost(Duration.ofSeconds(2)).until(() -> deleteService.getStatus() == DeleteStatus.RUNNING);

        assertThatThrownBy(() -> deleteService.startDeletion())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already running");

        deleteService.stopDeletion();
    }

    @Test
    void shouldThrowWhenStoppingWhileNotRunning() {
        assertThatThrownBy(() -> deleteService.stopDeletion())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No deletion process is currently running");
    }

    @Test
    void shouldReportIdleStatusInitially() {
        assertThat(deleteService.getStatus()).isEqualTo(DeleteStatus.IDLE);
        assertThat(deleteService.getDeletedCount()).isZero();
        assertThat(deleteService.getTotalCount()).isZero();
    }

    private AuditEventEntity buildEvent(long id) {
        ProcessStartedAuditEventEntity entity = new ProcessStartedAuditEventEntity();
        entity.setId(id);
        entity.setEventId("event-" + id);
        entity.setTimestamp(System.currentTimeMillis());
        return entity;
    }
}
