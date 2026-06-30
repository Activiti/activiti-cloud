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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatus;
import org.activiti.cloud.services.audit.jpa.model.AuditEventsDeletionStatusResponse;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "activiti.rest.enable-deletion=true")
@WebMvcTest(AuditEventsDeleteController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({ AlfrescoWebAutoConfiguration.class })
public class AuditEventDeleteControllerIT {

    @MockitoBean
    private AuditEventsDeletionService auditEventsDeletionService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private UserGroupManager userGroupManager;

    @BeforeEach
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("admin");
        assertThat(userGroupManager).isNotNull();
    }

    @Test
    public void deleteEventsShouldStartAsyncDeletionAndReturnAcceptedStatus() throws Exception {
        //given
        given(auditEventsDeletionService.startDeletion()).willReturn(true);
        given(auditEventsDeletionService.getStatusResponse())
            .willReturn(new AuditEventsDeletionStatusResponse(AuditEventsDeletionStatus.RUNNING, 0, 5, 5, 0.0));

        //when
        mockMvc
            .perform(
                delete("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            //then
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value("Audit events deletion started"))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.deletedCount").value(0))
            .andExpect(jsonPath("$.remainingCount").value(5))
            .andExpect(jsonPath("$.totalCount").value(5))
            .andExpect(jsonPath("$.percentComplete").value(0.0));

        verify(auditEventsDeletionService).deleteEventsAsync();
    }

    @Test
    public void deleteEventsShouldReturnConflictWhenDeletionIsAlreadyRunning() throws Exception {
        given(auditEventsDeletionService.startDeletion()).willReturn(false);
        given(auditEventsDeletionService.getStatusResponse())
            .willReturn(new AuditEventsDeletionStatusResponse(AuditEventsDeletionStatus.RUNNING, 2, 3, 5, 40.0));

        mockMvc
            .perform(
                delete("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Audit events deletion is already running"))
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    public void cancelDeletionShouldRequestCancellationAndReturnAcceptedStatus() throws Exception {
        given(auditEventsDeletionService.requestCancellation()).willReturn(true);
        given(auditEventsDeletionService.getStatusResponse())
            .willReturn(new AuditEventsDeletionStatusResponse(AuditEventsDeletionStatus.RUNNING, 2, 3, 5, 40.0));

        mockMvc
            .perform(
                post("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL + "/deletion/cancel").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value("Audit events deletion cancellation requested"))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.deletedCount").value(2))
            .andExpect(jsonPath("$.remainingCount").value(3));
    }

    @Test
    public void getDeletionStatusShouldReturnProgressInformation() throws Exception {
        given(auditEventsDeletionService.getStatusResponse())
            .willReturn(new AuditEventsDeletionStatusResponse(AuditEventsDeletionStatus.CANCELLED, 3, 2, 5, 60.0));

        mockMvc
            .perform(
                get("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL + "/deletion/status").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.deletedCount").value(3))
            .andExpect(jsonPath("$.remainingCount").value(2))
            .andExpect(jsonPath("$.totalCount").value(5))
            .andExpect(jsonPath("$.percentComplete").value(60.0));
    }
}
