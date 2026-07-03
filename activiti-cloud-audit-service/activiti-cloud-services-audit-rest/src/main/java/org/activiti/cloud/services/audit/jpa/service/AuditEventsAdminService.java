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

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsExporter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.exceptions.AuditExportException;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class AuditEventsAdminService {

    private record TimestampRange(long start, long end) {}

    private final EventsRepository eventsRepository;
    private final APIEventToEntityConverters eventConverters;
    private final AuditEventsExporter auditEventsExporter;

    public AuditEventsAdminService(
        EventsRepository eventsRepository,
        APIEventToEntityConverters eventConverters,
        AuditEventsExporter auditEventsExporter
    ) {
        this.eventsRepository = eventsRepository;
        this.eventConverters = eventConverters;
        this.auditEventsExporter = auditEventsExporter;
    }

    public Collection<AuditEventEntity> findAuditsBetweenDates(LocalDate fromDate, LocalDate toDate) {
        TimestampRange range = validateAndConvertDates(fromDate, toDate);
        return eventsRepository.findAllByTimestampBetweenOrderByTimestampDesc(range.start(), range.end());
    }

    public void exportAuditsBetweenDates(LocalDate fromDate, LocalDate toDate, HttpServletResponse response)
        throws IOException, AuditExportException {
        TimestampRange range = validateAndConvertDates(fromDate, toDate);

        try {
            CSVWriter csvWriter = auditEventsExporter.startExport(response);

            final int PAGE_SIZE = 1000;
            int pageNumber = 0;

            Page<AuditEventEntity> auditPage;
            do {
                Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "timestamp"));
                auditPage = eventsRepository.findAllByTimestampBetweenOrderByTimestampDesc(
                    range.start(),
                    range.end(),
                    pageable
                );

                if (auditPage == null || !auditPage.hasContent()) {
                    break;
                }

                List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = toCloudRuntimeEvents(auditPage.getContent());
                auditEventsExporter.writeChunk(csvWriter, events);

                pageNumber++;
            } while (auditPage.hasNext());

            auditEventsExporter.finishExport(csvWriter);
        } catch (CsvFieldAssignmentException | CsvChainedException e) {
            throw new AuditExportException("Failed writing CSV rows", e);
        }
    }

    private List<CloudRuntimeEvent<?, CloudRuntimeEventType>> toCloudRuntimeEvents(
        Iterable<? extends AuditEventEntity> auditEntities
    ) {
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events = new ArrayList<>();
        for (AuditEventEntity aee : auditEntities) {
            events.add(eventConverters.getConverterByEventTypeName(aee.getEventType()).convertToAPI(aee));
        }
        return events;
    }

    private TimestampRange validateAndConvertDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysBetween > 31) {
            throw new IllegalArgumentException("Difference between dates cannot be more than 31 days");
        }

        long startDateTime = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endDateTime = toDate.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        return new TimestampRange(startDateTime, endDateTime);
    }
}
