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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatus;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatusResponse;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;

public class AuditEventsDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventsDeletionService.class);

    private final EventsRepository<AuditEventEntity> eventsRepository;

    private final int batchSize;

    private final AtomicLong deletedCount = new AtomicLong(0);

    private final AtomicLong totalCount = new AtomicLong(0);

    private final AtomicReference<AuditEventsDeletionStatus> status = new AtomicReference<>(AuditEventsDeletionStatus.IDLE);

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    public AuditEventsDeletionService(
        EventsRepository<AuditEventEntity> eventsRepository,
        @Value("${activiti.audit.deletion.batch-size:100}") int batchSize
    ) {
        this.eventsRepository = eventsRepository;
        this.batchSize = batchSize;
    }

    public synchronized boolean startDeletion() {
        if (status.get() == AuditEventsDeletionStatus.RUNNING) {
            return false;
        }

        deletedCount.set(0);
        totalCount.set(eventsRepository.count());
        cancellationRequested.set(false);
        status.set(AuditEventsDeletionStatus.RUNNING);

        return true;
    }

    public boolean requestCancellation() {
        if (status.get() != AuditEventsDeletionStatus.RUNNING) {
            return false;
        }

        cancellationRequested.set(true);
        return true;
    }

    public AuditEventsDeletionStatusResponse getStatusResponse() {
        long total = totalCount.get();
        long deleted = deletedCount.get();
        long remaining = Math.max(total - deleted, 0);
        double percentComplete = total == 0 ? (status.get() == AuditEventsDeletionStatus.COMPLETED ? 100.0 : 0.0) : (deleted * 100.0) / total;

        return new AuditEventsDeletionStatusResponse(status.get(), deleted, remaining, total, percentComplete);
    }

    @Async
    public CompletableFuture<AuditEventsDeletionStatusResponse> deleteEventsAsync() {
        try {
            while (!cancellationRequested.get()) {
                Page<AuditEventEntity> eventsPage = eventsRepository.findAll(
                    PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "id"))
                );

                if (eventsPage.isEmpty()) {
                    status.set(AuditEventsDeletionStatus.COMPLETED);
                    return CompletableFuture.completedFuture(getStatusResponse());
                }

                for (AuditEventEntity event : eventsPage.getContent()) {
                    if (cancellationRequested.get()) {
                        status.set(AuditEventsDeletionStatus.CANCELLED);
                        return CompletableFuture.completedFuture(getStatusResponse());
                    }

                    eventsRepository.deleteById(event.getId());
                    deletedCount.incrementAndGet();
                }
            }

            status.set(AuditEventsDeletionStatus.CANCELLED);
            return CompletableFuture.completedFuture(getStatusResponse());
        } catch (Exception ex) {
            LOGGER.error("Failed to delete audit events asynchronously", ex);
            status.set(AuditEventsDeletionStatus.FAILED);
            return CompletableFuture.completedFuture(getStatusResponse());
        }
    }
}
