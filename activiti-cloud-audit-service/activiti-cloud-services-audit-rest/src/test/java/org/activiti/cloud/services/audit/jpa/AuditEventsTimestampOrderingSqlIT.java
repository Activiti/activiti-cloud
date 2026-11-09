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
package org.activiti.cloud.services.audit.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsAdminService;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Renders SQL with the PostgreSQL dialect and the global NULLS LAST default used in production, so that
 * timestamp DESC ordering keeps matching the existing ascending timestamp index (backward scan).
 */
@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.order_by.default_null_ordering=last",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.activiti.cloud.services.audit.jpa.AuditEventsTimestampOrderingSqlIT$CapturedStatements",
    }
)
@AutoConfigureMockMvc
@Import({ AlfrescoWebAutoConfiguration.class, AuditTestConfiguration.class })
class AuditEventsTimestampOrderingSqlIT {

    public static class CapturedStatements implements StatementInspector {

        private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            STATEMENTS.add(sql);
            return sql;
        }

        static void clear() {
            STATEMENTS.clear();
        }

        static String findFirst(Predicate<String> filter) {
            return STATEMENTS.stream()
                .map(sql -> sql.toLowerCase(Locale.ROOT))
                .filter(filter)
                .findFirst()
                .orElseThrow();
        }
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    ).withZone(ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventsAdminService auditEventsAdminService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private SecurityManager securityManager;

    @BeforeEach
    void clearCapturedStatements() {
        CapturedStatements.clear();
    }

    @Test
    void should_notRenderNullOrdering_when_searchingV2EventsWithDefaultSort() throws Exception {
        mockMvc
            .perform(
                get("/v2/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("eventTimeFrom", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(1000L)))
                    .param("eventTimeTo", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(3000L)))
            )
            .andExpect(status().isOk());

        String sql = CapturedStatements.findFirst(
            statement -> statement.contains("from audit_event") && statement.contains("order by")
        );

        assertThat(sql).containsPattern("order by \\S*timestamp desc").doesNotContain("nulls");
    }

    @Test
    void should_notRenderNullOrdering_when_searchingV2EventsWithExplicitDescendingSort() throws Exception {
        mockMvc
            .perform(
                get("/v2/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("sort", "timestamp,desc")
                    .param("eventTimeFrom", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(1000L)))
                    .param("eventTimeTo", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(3000L)))
            )
            .andExpect(status().isOk());

        String sql = CapturedStatements.findFirst(
            statement -> statement.contains("from audit_event") && statement.contains("order by")
        );

        assertThat(sql).containsPattern("order by \\S*timestamp desc").doesNotContain("nulls");
    }

    @Test
    void should_notRenderNullOrdering_when_searchingV2EventsWithExplicitAscendingSort() throws Exception {
        mockMvc
            .perform(
                get("/v2/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("sort", "timestamp,asc")
                    .param("eventTimeFrom", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(1000L)))
                    .param("eventTimeTo", DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(3000L)))
            )
            .andExpect(status().isOk());

        String sql = CapturedStatements.findFirst(
            statement -> statement.contains("from audit_event") && statement.contains("order by")
        );

        assertThat(sql).containsPattern("order by \\S*timestamp(?: asc)? offset").doesNotContain("nulls");
    }

    @Test
    void should_notRenderNullOrdering_when_exportingEventsBetweenDates() throws Exception {
        auditEventsAdminService.exportAuditsBetweenDates(
            LocalDate.of(2024, 7, 22),
            LocalDate.of(2024, 7, 24),
            new MockHttpServletResponse()
        );

        String sql = CapturedStatements.findFirst(
            statement ->
                statement.contains("from audit_event") &&
                statement.contains("between") &&
                statement.contains("order by")
        );

        assertThat(sql).containsPattern("order by \\S*timestamp desc").doesNotContain("nulls");
    }
}
