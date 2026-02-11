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

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.createProcessVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.common.error.attributes.ErrorAttributesMessageSanitizer;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ProcessInstanceController.class)
@Import(
    { QueryRestWebMvcAutoConfiguration.class, CommonModelAutoConfiguration.class, AlfrescoWebAutoConfiguration.class }
)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(
    locations = { "classpath:application-test.properties" },
    properties = "activiti.cloud.rest.max-items.enabled=true"
)
class ProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private EntityFinder entityFinder;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockitoBean
    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private TaskControllerHelper taskControllerHelper;

    @MockitoBean
    private VariableRepository processVariableRepository;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @BeforeEach
    void setUp() {
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    void shouldReturnProcessInstancesWithoutVariableKeys() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        processInstanceEntity.setLinkedProcessInstanceId("123-456-789-1111");
        processInstanceEntity.setLinkedProcessInstanceType("my-type");
        Page<ProcessInstanceEntity> processInstancePage = new PageImpl<>(
            Collections.singletonList(processInstanceEntity),
            PageRequest.of(1, 10),
            1
        );
        given(processInstanceRestrictionService.restrictProcessInstanceQuery(any(), eq(SecurityPolicyAccess.READ)))
            .willReturn(restrictedPredicate);
        given(processInstanceRepository.findAll(any(Predicate.class), any(Pageable.class)))
            .willReturn(processInstancePage);
        given(processInstanceRepository.mapSubprocesses(any(), any(Pageable.class))).willReturn(processInstancePage);

        //when
        mockMvc
            .perform(get("/v1/process-instances?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.id").value(processInstanceEntity.getId()))
            .andExpect(jsonPath("$.list.entries[0].entry.status").value(processInstanceEntity.getStatus().name()))
            .andExpect(jsonPath("$.list.entries[0].entry.serviceName").value(processInstanceEntity.getServiceName()))
            .andExpect(
                jsonPath("$.list.entries[0].entry.linkedProcessInstanceId")
                    .value(processInstanceEntity.getLinkedProcessInstanceId())
            )
            .andExpect(
                jsonPath("$.list.entries[0].entry.linkedProcessInstanceType")
                    .value(processInstanceEntity.getLinkedProcessInstanceType())
            );
    }

    @Test
    void shouldReturnProcessInstancesWithVariableKeys() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 6);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).toList();
        List<String> ids = Collections.singletonList(processInstanceEntity.getId());

        Page<ProcessInstanceEntity> processInstancePage = new PageImpl<>(
            Collections.singletonList(processInstanceEntity),
            PageRequest.of(1, 10),
            1
        );
        given(processInstanceRestrictionService.restrictProcessInstanceQuery(any(), eq(SecurityPolicyAccess.READ)))
            .willReturn(restrictedPredicate);
        given(processInstanceRepository.findByIdIsIn(ids, Sort.unsorted()))
            .willReturn(Collections.singletonList(processInstanceEntity));
        given(processInstanceRepository.mapSubprocesses(any(), any(Pageable.class))).willReturn(processInstancePage);

        //when
        mockMvc
            .perform(
                get("/v1/process-instances?variableKeys={variableKeys}&skipCount=10&maxItems=10", variableKeys)
                    .accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.id").value(processInstanceEntity.getId()))
            .andExpect(jsonPath("$.list.entries[0].entry.status").value(processInstanceEntity.getStatus().name()))
            .andExpect(jsonPath("$.list.entries[0].entry.serviceName").value(processInstanceEntity.getServiceName()));
    }

    @Test
    void shouldReturnProcessInstanceById() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        String processInstanceId = processInstanceEntity.getId();

        given(processInstanceRestrictionService.restrictProcessInstanceQuery(any(), eq(SecurityPolicyAccess.READ)))
            .willReturn(restrictedPredicate);
        given(processInstanceService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        mockMvc
            .perform(
                get("/v1/process-instances/{processInstanceId}", processInstanceId).accept(MediaType.APPLICATION_JSON)
            )
            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entry.id").value(processInstanceEntity.getId()))
            .andExpect(jsonPath("$.entry.status").value(processInstanceEntity.getStatus().name()));
    }

    @Test
    void should_returnBadRequestError_when_invalidProcessInstanceEnum() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/v1/process-instances?status=ASSIGNED").accept(MediaType.APPLICATION_JSON))
            //then
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString())
            .contains(ErrorAttributesMessageSanitizer.ERROR_NOT_DISCLOSED_MESSAGE);
    }

    @Test
    void should_returnOk_when_linkProcessInstances() throws Exception {
        //when
        mockMvc
            .perform(
                post("/v1/process-instances/{mainProcessInstanceId}/link", "mainProcessInstanceId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "processInstanceIds": ["%s", "%s"],
                            "linkProcessInstanceType": "%s"
                        }
                        """.formatted(
                                "orphanProcessInstanceId1",
                                "orphanProcessInstanceId2",
                                "linkType"
                            )
                    )
                    .with(csrf())
            )
            //then
            .andExpect(status().isOk());
    }

    @Test
    void should_return_InternalServerError_when_linkProcessInstances() throws Exception {
        RuntimeException generic = new RuntimeException("boom");
        doThrow(generic)
            .when(processInstanceService)
            .linkProcessInstances(
                "mainProcessInstanceId",
                List.of("orphanProcessInstanceId1", "orphanProcessInstanceId2"),
                "linkType"
            );

        //when
        mockMvc
            .perform(
                post("/v1/process-instances/{mainProcessInstanceId}/link", "mainProcessInstanceId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "processInstanceIds": ["%s", "%s"],
                            "linkProcessInstanceType": "%s"
                        }
                        """.formatted(
                                "orphanProcessInstanceId1",
                                "orphanProcessInstanceId2",
                                "linkType"
                            )
                    )
                    .with(csrf())
            )
            //then
            .andExpect(status().isInternalServerError());
    }

    @Test
    void should_return_BadRequest_when_linkProcessInstances_with_invalid_request() throws Exception {
        //when
        mockMvc
            .perform(
                post("/v1/process-instances/{mainProcessInstanceId}/link", "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "processInstanceIds": ["%s", "%s"],
                            "linkProcessInstanceType": "%s"
                        }
                        """.formatted(
                                "orphanProcessInstanceId1",
                                "orphanProcessInstanceId2",
                                "linkType"
                            )
                    )
                    .with(csrf())
            )
            //then
            .andExpect(status().isBadRequest());
    }
}
