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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

public class AuditEventsDeleteService {

    public enum DeleteStatus {
        IDLE,
        RUNNING,
        STOPPED,
        COMPLETED,
    }

    private final EventsRepository eventsRepository;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile DeleteStatus status = DeleteStatus.IDLE;
    private final AtomicLong deletedCount = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);

    public AuditEventsDeleteService(
        EventsRepository eventsRepository,
        TransactionTemplate transactionTemplate,
        int batchSize
    ) {
        this.eventsRepository = eventsRepository;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = batchSize;
    }

    public synchronized void startDeletion() {
        if (status == DeleteStatus.RUNNING) {
            throw new IllegalStateException("A deletion process is already running");
        }

        cancelRequested.set(false);
        deletedCount.set(0);
        totalCount.set(eventsRepository.count());
        status = DeleteStatus.RUNNING;

        Thread.ofVirtual().name("audit-events-delete").start(this::executeDeletion);
    }

    public void stopDeletion() {
        if (status != DeleteStatus.RUNNING) {
            throw new IllegalStateException("No deletion process is currently running");
        }
        cancelRequested.set(true);
    }

    public DeleteStatus getStatus() {
        return status;
    }

    public long getDeletedCount() {
        return deletedCount.get();
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    private void executeDeletion() {
        try {
            while (!cancelRequested.get()) {
                Boolean hasMore = transactionTemplate.execute(txStatus -> {
                    Page<AuditEventEntity> page = eventsRepository.findAll(PageRequest.of(0, batchSize));
                    if (page.isEmpty()) {
                        return false;
                    }
                    eventsRepository.deleteAll(page.getContent());
                    deletedCount.addAndGet(page.getNumberOfElements());
                    return page.hasNext() || page.getNumberOfElements() == batchSize;
                });

                if (Boolean.FALSE.equals(hasMore) || hasMore == null) {
                    break;
                }
            }

            status = cancelRequested.get() ? DeleteStatus.STOPPED : DeleteStatus.COMPLETED;
        } catch (Exception e) {
            status = DeleteStatus.STOPPED;
        }
    }
}
