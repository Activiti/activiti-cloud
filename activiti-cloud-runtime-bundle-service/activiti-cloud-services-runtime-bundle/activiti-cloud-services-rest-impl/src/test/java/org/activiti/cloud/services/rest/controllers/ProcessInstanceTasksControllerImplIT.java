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

import static org.activiti.cloud.services.rest.controllers.PageConverterTestUtils.setupPageConverterStub;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildDefaultAssignedTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.runtime.TaskAdminRuntime;
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
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessInstanceTasksControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import(
    {
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
class ProcessInstanceTasksControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private TaskRuntime taskRuntime;

    @MockitoBean
    private SpringPageConverter pageConverter;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

    @MockitoBean
    private TaskAdminRuntime taskAdminRuntime;

    @MockitoBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockitoBean(name = ProcessEngineChannels.COMMAND_RESULTS)
    private MessageChannel commandResults;

    @MockitoBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockitoBean
    private SecurityContextPrincipalProvider securityContextPrincipalProvider;

    @MockitoBean
    private RuntimeService runtimeService;

    @MockitoBean
    private PrincipalIdentityProvider principalIdentityProvider;

    @MockitoBean
    private ManagementService managementService;

    @BeforeEach
    void setUp() {
        assertThat(pageConverter).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();

        setupPageConverterStub(pageConverter);
    }

    @Test
    void getTasks() throws Exception {
        List<Task> taskList = Collections.singletonList(buildDefaultAssignedTask());
        Page<Task> tasks = new PageImpl<>(taskList, taskList.size());

        when(taskRuntime.tasks(any(), any())).thenReturn(tasks);

        this.mockMvc.perform(
                get("/v1/process-instances/{processInstanceId}/tasks?page=10&size=10", 1, 1)
                    .accept(MediaTypes.HAL_JSON_VALUE)
            )
            .andExpect(status().isOk());
    }

    @Test
    void getTasksShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        Task task = buildDefaultAssignedTask();
        List<Task> taskList = Collections.singletonList(task);
        Page<Task> taskPage = new PageImpl<>(taskList, taskList.size());

        when(taskRuntime.tasks(any(), any())).thenReturn(taskPage);

        this.mockMvc.perform(
                get(
                    "/v1/process-instances/{processInstanceId}/tasks?skipCount=10&maxItems=10",
                    task.getProcessInstanceId(),
                    1
                )
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }
}
