/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;

public class AuditEventsAdminService {
    private final EventsRepository eventsRepository;

    public AuditEventsAdminService(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    public Collection<AuditEventEntity> findAuditsBetweenDates(LocalDate fromDate, LocalDate toDate) {
        if(fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);

        if(daysBetween > 31 || daysBetween < 0) {
            throw new IllegalArgumentException("Difference between dates cannot be more than 31 days or negative");
        }

        Long startDateTime = fromDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        Long endDateTime = toDate.atStartOfDay().plusDays(1).toEpochSecond(ZoneOffset.UTC);

        return eventsRepository.findAllByTimestampBetweenOrderByTimestampDesc(startDateTime, endDateTime);
    }
}
