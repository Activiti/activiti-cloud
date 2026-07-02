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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class AuditEventsAdminService {

    private final EventsRepository eventsRepository;

    public AuditEventsAdminService(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public Collection<AuditEventEntity> findAuditsBetweenDates(LocalDate fromDate, LocalDate toDate) {
        Long[] timestamps = validateAndConvertDates(fromDate, toDate);
        return eventsRepository.findAllByTimestampBetweenOrderByTimestampDesc(timestamps[0], timestamps[1]);
    }

    public Page<AuditEventEntity> findAuditsBetweenDates(
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
    ) {
        Long[] timestamps = validateAndConvertDates(fromDate, toDate);
        return eventsRepository.findAllByTimestampBetweenOrderByTimestampDesc(timestamps[0], timestamps[1], pageable);
    }

    private Long[] validateAndConvertDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysBetween > 31) {
            throw new IllegalArgumentException("Difference between dates cannot be more than 31 days");
        }

        Long startDateTime = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        Long endDateTime = toDate.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        return new Long[] { startDateTime, endDateTime };
    }
}
