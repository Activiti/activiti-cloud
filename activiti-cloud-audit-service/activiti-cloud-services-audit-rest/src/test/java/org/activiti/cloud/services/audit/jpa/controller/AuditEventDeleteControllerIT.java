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
import static org.mockito.Mockito.doThrow;
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
import org.activiti.cloud.services.audit.api.config.AuditAPIAutoConfiguration;
import org.activiti.cloud.services.audit.api.resources.EventsLinkRelationProvider;
import org.activiti.cloud.services.audit.jpa.assembler.config.EventRepresentationModelAssemblerConfiguration;
import org.activiti.cloud.services.audit.jpa.conf.AuditJPAAutoConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeleteService;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsDeleteService.DeleteStatus;
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
@Import(
    {
        EventRepresentationModelAssemblerConfiguration.class,
        AuditAPIAutoConfiguration.class,
        AuditJPAAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class,
    }
)
public class AuditEventDeleteControllerIT {

    @MockitoBean
    private AuditEventsDeleteService deleteService;

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
    public void deleteEventsShouldStartDeletionAndReturnAccepted() throws Exception {
        when(deleteService.getStatus()).thenReturn(DeleteStatus.RUNNING);
        when(deleteService.getDeletedCount()).thenReturn(0L);
        when(deleteService.getTotalCount()).thenReturn(100L);

        mockMvc
            .perform(
                delete("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.totalCount").value(100));

        verify(deleteService).startDeletion();
    }

    @Test
    public void deleteEventsShouldReturnConflictWhenAlreadyRunning() throws Exception {
        doThrow(new IllegalStateException("A deletion process is already running"))
            .when(deleteService)
            .startDeletion();

        mockMvc
            .perform(
                delete("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isConflict());
    }

    @Test
    public void stopDeleteEventsShouldStopRunningDeletion() throws Exception {
        when(deleteService.getStatus()).thenReturn(DeleteStatus.STOPPED);
        when(deleteService.getDeletedCount()).thenReturn(50L);
        when(deleteService.getTotalCount()).thenReturn(100L);

        mockMvc
            .perform(
                post("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL + "/delete/stop").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STOPPED"))
            .andExpect(jsonPath("$.deletedCount").value(50));

        verify(deleteService).stopDeletion();
    }

    @Test
    public void stopDeleteEventsShouldReturnConflictWhenNotRunning() throws Exception {
        doThrow(new IllegalStateException("No deletion process is currently running"))
            .when(deleteService)
            .stopDeletion();

        mockMvc
            .perform(
                post("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL + "/delete/stop").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isConflict());
    }

    @Test
    public void getDeleteStatusShouldReturnCurrentStatus() throws Exception {
        when(deleteService.getStatus()).thenReturn(DeleteStatus.COMPLETED);
        when(deleteService.getDeletedCount()).thenReturn(100L);
        when(deleteService.getTotalCount()).thenReturn(100L);

        mockMvc
            .perform(
                get("/admin/v1/" + EventsLinkRelationProvider.COLLECTION_RESOURCE_REL + "/delete/status").accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.deletedCount").value(100))
            .andExpect(jsonPath("$.totalCount").value(100));
    }
}
