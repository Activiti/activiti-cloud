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
package org.activiti.cloud.services.rest.conf;

import static org.activiti.cloud.services.rest.controllers.ProcessInstanceSamples.defaultProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.ProcessDefinitionsSyncService;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.ProcessEngineChannelsConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.config.StreamConfig;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest({ ProcessInstanceControllerImpl.class, ProcessInstanceVariableControllerImpl.class })
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
        ServicesCoreAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class,
        StreamConfig.class,
        CacheAutoConfiguration.class,
        VariableRequestSizeLimitAutoConfiguration.class,
    }
)
@TestPropertySource(properties = "activiti.cloud.services.variables.max-request-size-bytes=256")
class VariableRequestSizeLimitFilterIT {

    private static final int MAX_SIZE_BYTES = 256;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ProcessRuntime processRuntime;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private TaskAdminRuntime taskAdminRuntime;

    @MockitoBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockitoBean(name = ProcessEngineChannels.COMMAND_RESULTS)
    private MessageChannel commandResults;

    @MockitoBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @MockitoBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockitoBean
    private ProcessExtensionService processExtensionService;

    @MockitoBean
    private SecurityContextPrincipalProvider securityContextPrincipalProvider;

    @MockitoBean
    private RuntimeService runtimeService;

    @MockitoBean
    private PrincipalIdentityProvider principalIdentityProvider;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private ProcessDefinitionsSyncService processDefinitionsSyncService;

    @MockitoSpyBean
    private CollectionModelAssembler collectionModelAssembler;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

    // --- Start process endpoint: POST /v1/process-instances ---

    @Test
    void should_rejectOversizedBody_forStartProcessEndpoint() {
        byte[] oversizedBody = new byte[MAX_SIZE_BYTES + 100];
        Arrays.fill(oversizedBody, (byte) 'a');

        assertThatThrownBy(() ->
            mockMvc.perform(post("/v1/process-instances").contentType(APPLICATION_JSON).content(oversizedBody))
        )
            .rootCause()
            .isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void should_allowWithinLimitBody_forStartProcessEndpoint() throws Exception {
        when(processRuntime.start(any(StartProcessPayload.class))).thenReturn(defaultProcessInstance());

        StartProcessPayload cmd = ProcessPayloadBuilder.start().withProcessDefinitionId("1").build();

        mockMvc
            .perform(
                post("/v1/process-instances").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isOk());
    }

    // --- Set variables endpoint: PUT /v1/process-instances/{id}/variables ---

    @Test
    void should_rejectOversizedBody_forSetVariablesEndpoint() {
        byte[] oversizedBody = new byte[MAX_SIZE_BYTES + 100];
        Arrays.fill(oversizedBody, (byte) 'a');

        assertThatThrownBy(() ->
            mockMvc.perform(
                put("/v1/process-instances/{processInstanceId}/variables", UUID.randomUUID().toString())
                    .contentType(APPLICATION_JSON)
                    .content(oversizedBody)
            )
        )
            .rootCause()
            .isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void should_allowWithinLimitBody_forSetVariablesEndpoint() throws Exception {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");
        when(processRuntime.processInstance(any())).thenReturn(processInstance);

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "val1");

        mockMvc
            .perform(
                put("/v1/process-instances/{processInstanceId}/variables", "1")
                    .contentType(APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            ProcessPayloadBuilder.setVariables()
                                .withProcessInstanceId("1")
                                .withVariables(variables)
                                .build()
                        )
                    )
            )
            .andExpect(status().isOk());
    }

    // --- Filter should not apply to non-variable endpoints ---

    @Test
    void should_notApplyFilter_forUpdateProcessEndpoint() throws Exception {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");
        when(processRuntime.processInstance(any())).thenReturn(processInstance);
        when(processRuntime.update(any())).thenReturn(defaultProcessInstance());

        // Build a valid UpdateProcessPayload that exceeds the size limit by padding the name
        char[] padding = new char[MAX_SIZE_BYTES + 100];
        Arrays.fill(padding, 'x');
        String longName = new String(padding);

        String body = mapper.writeValueAsString(
            ProcessPayloadBuilder.update().withProcessInstanceId("1").withName(longName).build()
        );

        // PUT /v1/process-instances/{id} is the update endpoint, not a variable endpoint.
        // The filter should not apply, so the oversized body passes through to the controller.
        mockMvc
            .perform(put("/v1/process-instances/{processInstanceId}", "1").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }
}
