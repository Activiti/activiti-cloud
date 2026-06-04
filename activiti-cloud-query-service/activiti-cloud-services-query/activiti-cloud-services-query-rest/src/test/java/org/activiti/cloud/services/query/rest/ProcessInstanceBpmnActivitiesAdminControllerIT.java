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
package org.activiti.cloud.services.query.rest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.Date;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ProcessInstanceBpmnActivitiesAdminController.class)
@EnableSpringDataWebSupport
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@WithMockUser
class ProcessInstanceBpmnActivitiesAdminControllerIT {

    private static final String PROCESS_INSTANCE_ID = "process-instance-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BPMNActivityRepository bpmnActivityRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private VariableRepository variableRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        assertThat(processInstanceAdminService).isNotNull();
        assertThat(processInstanceService).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    void shouldReturnActivitiesJsonWhenAcceptIsApplicationJson() throws Exception {
        BPMNActivityEntity activity = buildActivity();

        given(bpmnActivityRepository.findAll(any(Predicate.class), any(Pageable.class)))
            .willReturn(
                new PageImpl<>(
                    Collections.singletonList(activity),
                    new AlfrescoPageRequest(0, 10, PageRequest.of(0, 10)),
                    1
                )
            );

        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}/bpmn-activities", PROCESS_INSTANCE_ID)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();

        assertThatJson(body).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(body)
            .inPath("list.entries[0].entry.id")
            .isEqualTo(PROCESS_INSTANCE_ID + ":start-event-1:execution-1");
        assertThatJson(body).inPath("list.entries[0].entry.elementId").isEqualTo("start-event-1");
        assertThatJson(body).inPath("list.entries[0].entry.activityType").isEqualTo("startEvent");
        assertThatJson(body).inPath("list.entries[0].entry.status").isEqualTo("COMPLETED");
        assertThatJson(body).inPath("list.entries[0].entry.executionId").isEqualTo("execution-1");
        assertThatJson(body).inPath("list.entries[0].entry.startedDate").isString();
        assertThatJson(body).inPath("list.entries[0].entry.completedDate").isString();

        // Diagram-only projection: process / service metadata must be omitted.
        assertThatJson(body).inPath("list.entries[0].entry.serviceName").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.serviceFullName").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.serviceVersion").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.appName").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.appVersion").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.processInstanceId").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.processDefinitionId").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.processDefinitionKey").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.processDefinitionVersion").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.activityName").isAbsent();
        assertThatJson(body).inPath("list.entries[0].entry.businessKey").isAbsent();
        // cancelledDate is null on a COMPLETED activity and must be omitted.
        assertThatJson(body).inPath("list.entries[0].entry.cancelledDate").isAbsent();
    }

    @Test
    void shouldReturnEmptyActivitiesListWhenNoneExist() throws Exception {
        given(bpmnActivityRepository.findAll(any(Predicate.class), any(Pageable.class)))
            .willReturn(
                new PageImpl<>(Collections.emptyList(), new AlfrescoPageRequest(0, 10, PageRequest.of(0, 10)), 0)
            );

        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}/bpmn-activities", PROCESS_INSTANCE_ID)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(0);
    }

    private BPMNActivityEntity buildActivity() {
        BPMNActivityEntity activity = new BPMNActivityEntity(
            "rb-service-name",
            "rb-service-full-name",
            "rb-service-version",
            "app-name",
            "app-version"
        );
        activity.setId(PROCESS_INSTANCE_ID + ":" + "start-event-1" + ":execution-1");
        activity.setProcessInstanceId(PROCESS_INSTANCE_ID);
        activity.setElementId("start-event-1");
        activity.setActivityType("startEvent");
        activity.setActivityName("start-event-1");
        activity.setProcessDefinitionId("process-definition-1");
        activity.setExecutionId("execution-1");
        activity.setStatus(BPMNActivityStatus.COMPLETED);
        activity.setStartedDate(new Date(1_700_000_000_000L));
        activity.setCompletedDate(new Date(1_700_000_005_000L));
        return activity;
    }
}
