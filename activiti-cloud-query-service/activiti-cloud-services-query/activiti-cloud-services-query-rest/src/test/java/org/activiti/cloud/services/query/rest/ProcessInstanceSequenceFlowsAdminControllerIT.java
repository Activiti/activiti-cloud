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
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

@WebMvcTest(ProcessInstanceSequenceFlowsAdminController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@WithMockUser
class ProcessInstanceSequenceFlowsAdminControllerIT {

    private static final String PROCESS_INSTANCE_ID = "process-instance-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

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

    @Test
    void shouldReturnSequenceFlowsJsonWhenAcceptIsApplicationJson() throws Exception {
        BPMNSequenceFlowEntity sequenceFlow = buildSequenceFlow();

        given(bpmnSequenceFlowRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(
            new PageImpl<>(
                Collections.singletonList(sequenceFlow),
                new AlfrescoPageRequest(0, 10, PageRequest.of(0, 10)),
                1
            )
        );

        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}/sequence-flows", PROCESS_INSTANCE_ID).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();

        assertThatJson(body).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(body).inPath("list.entries[0].entry.elementId").isEqualTo("flow-1");
        assertThatJson(body).inPath("list.entries[0].entry.sourceActivityElementId").isEqualTo("start-event-1");
        assertThatJson(body).inPath("list.entries[0].entry.targetActivityElementId").isEqualTo("end-event-1");
    }

    @Test
    void shouldReturnEmptySequenceFlowsListWhenNoneExist() throws Exception {
        given(bpmnSequenceFlowRepository.findAll(any(Predicate.class), any(Pageable.class))).willReturn(
            new PageImpl<>(Collections.emptyList(), new AlfrescoPageRequest(0, 10, PageRequest.of(0, 10)), 0)
        );

        MvcResult result = mockMvc
            .perform(
                get("/admin/v1/process-instances/{processInstanceId}/sequence-flows", PROCESS_INSTANCE_ID).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andReturn();

        assertThatJson(result.getResponse().getContentAsString()).inPath("list.pagination.count").isEqualTo(0);
    }

    private BPMNSequenceFlowEntity buildSequenceFlow() {
        BPMNSequenceFlowEntity sequenceFlow = new BPMNSequenceFlowEntity(
            "rb-service-name",
            "rb-service-full-name",
            "rb-service-version",
            "app-name",
            "app-version"
        );
        sequenceFlow.setId(PROCESS_INSTANCE_ID + ":" + "flow-1");
        sequenceFlow.setProcessInstanceId(PROCESS_INSTANCE_ID);
        sequenceFlow.setProcessDefinitionId("process-definition-1");
        sequenceFlow.setElementId("flow-1");
        sequenceFlow.setSourceActivityElementId("start-event-1");
        sequenceFlow.setSourceActivityName("Start");
        sequenceFlow.setSourceActivityType("startEvent");
        sequenceFlow.setTargetActivityElementId("end-event-1");
        sequenceFlow.setTargetActivityName("End");
        sequenceFlow.setTargetActivityType("endEvent");
        sequenceFlow.setEventId("event-1");
        sequenceFlow.setDate(new Date());
        return sequenceFlow;
    }
}
