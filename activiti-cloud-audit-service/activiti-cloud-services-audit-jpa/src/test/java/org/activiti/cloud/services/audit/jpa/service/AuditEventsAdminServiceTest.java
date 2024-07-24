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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
public class AuditEventsAdminServiceTest {

    @Mock
    private EventsRepository eventsRepository;

    @InjectMocks
    private AuditEventsAdminService auditEventsAdminService;

    @Test
    void should_throw_exception_when_from_date_is_after_to_date() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2019, 1, 1);

        // when
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate)
        );

        // then
        assertEquals("From date cannot be after to date", thrown.getMessage());
    }

    @Test
    void should_throw_exception_when_difference_between_dates_is_more_than_31_days() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2020, 2, 1);

        // when
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate)
        );

        // then
        assertEquals("Difference between dates cannot be more than 31 days or negative", thrown.getMessage());
    }

    @Test
    void should_throw_exception_when_difference_between_dates_is_negative() {
        // given
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2019, 12, 1);

        // when
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> auditEventsAdminService.findAuditsBetweenDates(fromDate, toDate)
        );

        // then
        assertEquals("Difference between dates cannot be more than 31 days or negative", thrown.getMessage());
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
}
