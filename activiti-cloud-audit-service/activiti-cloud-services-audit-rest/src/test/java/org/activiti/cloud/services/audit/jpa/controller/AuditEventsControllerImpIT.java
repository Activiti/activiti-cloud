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
package org.activiti.cloud.services.audit.jpa.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.AuditTestConfiguration;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ActivityStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.util.TestConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = { "spring.main.banner-mode=off" })
@AutoConfigureMockMvc
@Import({ AlfrescoWebAutoConfiguration.class, AuditTestConfiguration.class })
class AuditEventsControllerImpIT {

    @Autowired
    private EventsRepository<AuditEventEntity> eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private SecurityManager securityManager;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    private static final String ENTRIES_ROOT = "$._embedded.events";
    private static final String EVENTS_ID_ROOT = "$._embedded.events[*].id";

    @AfterEach
    void cleanUp() {
        eventsRepository.deleteAll();
    }

    @Test
    void should_returnAuditEvents_filteredByEventTimeFrom() throws Exception {
        AuditEventEntity audit1 = new ProcessStartedAuditEventEntity();
        audit1.setTimestamp(1000L);
        audit1.setEventType(TestConverter.EVENT_TYPE);

        AuditEventEntity audit2 = new ActivityStartedAuditEventEntity();
        audit2.setTimestamp(2000L);
        audit2.setEventType(TestConverter.EVENT_TYPE);

        AuditEventEntity audit3 = new ActivityCompletedAuditEventEntity();
        audit3.setTimestamp(3000L);
        audit3.setEventType(TestConverter.EVENT_TYPE);

        eventsRepository.saveAll(List.of(audit1, audit2, audit3));

        mockMvc
            .perform(
                get("/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param(
                        "eventTimeFrom",
                        dateTimeFormatter.format(Instant.ofEpochMilli(2000L).atZone(ZoneOffset.UTC))
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(ENTRIES_ROOT, hasSize(2)))
            .andExpect(jsonPath(EVENTS_ID_ROOT, contains(audit3.getId().toString(), audit2.getId().toString())));

        eventsRepository.deleteAll();
    }

    @Test
    void should_returnAuditEvents_filteredByEventTimeTo() throws Exception {
        AuditEventEntity audit1 = new ProcessStartedAuditEventEntity();
        audit1.setTimestamp(1000L);
        audit1.setEventType(TestConverter.EVENT_TYPE);

        AuditEventEntity audit2 = new ActivityStartedAuditEventEntity();
        audit2.setTimestamp(2000L);
        audit2.setEventType(TestConverter.EVENT_TYPE);

        AuditEventEntity audit3 = new ActivityCompletedAuditEventEntity();
        audit3.setTimestamp(3000L);
        audit3.setEventType(TestConverter.EVENT_TYPE);

        eventsRepository.saveAll(List.of(audit1, audit2, audit3));

        mockMvc
            .perform(
                get("/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("eventTimeTo", dateTimeFormatter.format(Instant.ofEpochMilli(2000L).atZone(ZoneOffset.UTC)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(ENTRIES_ROOT, hasSize(2)))
            .andExpect(jsonPath(EVENTS_ID_ROOT, contains(audit2.getId().toString(), audit1.getId().toString())));
    }
}
