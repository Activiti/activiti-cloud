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
package org.activiti.cloud.services.rest.controllers;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.activiti.cloud.services.rest.controllers.PageConverterTestUtils.setupPageConverterStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.ProcessEngineChannelsConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.rest.config.StreamConfig;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(CandidateUserControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import(
    {
        CommonModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ProcessEngineChannelsConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class,
        StreamConfig.class,
    }
)
class CandidateUserControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private TaskRuntime taskRuntime;

    @MockitoBean
    private SpringPageConverter springPageConverter;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

    @MockitoBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockitoBean
    private SecurityContextPrincipalProvider securityContextPrincipalProvider;

    @MockitoBean
    private RuntimeService runtimeService;

    @MockitoBean
    private PrincipalIdentityProvider principalIdentityProvider;

    @MockitoBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockitoBean
    private ManagementService managementService;

    @BeforeEach
    void setUp() {
        assertThat(springPageConverter).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();

        setupPageConverterStub(springPageConverter);
    }

    @Test
    void getUserCandidatesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        List<String> stringList = Arrays.asList("hruser", "testuser");
        when(taskRuntime.userCandidates("1")).thenReturn(stringList);

        MvcResult result =
            this.mockMvc.perform(get("/v1/tasks/{taskId}/candidate-users", 1).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.entries[0].entry.user")
            .isEqualTo("hruser");
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("list.entries[1].entry.user")
            .isEqualTo("testuser");
    }

    @Test
    void getUserCandidatesShouldHaveProperHALFormat() throws Exception {
        List<String> stringList = Arrays.asList("hruser", "testuser");
        when(taskRuntime.userCandidates("1")).thenReturn(stringList);

        MvcResult result =
            this.mockMvc.perform(get("/v1/tasks/{taskId}/candidate-users", 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
            .inPath("_embedded.candidateUsers[0].user")
            .isEqualTo("hruser");
        assertThatJson(result.getResponse().getContentAsString())
            .inPath("_embedded.candidateUsers[1].user")
            .isEqualTo("testuser");
    }
}
