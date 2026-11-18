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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.api.task.runtime.TaskRuntime;
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
import org.activiti.cloud.services.rest.controllers.TaskVariableAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest({
    ProcessInstanceControllerImpl.class,
    ProcessInstanceVariableControllerImpl.class,
    TaskVariableControllerImpl.class,
    TaskVariableAdminControllerImpl.class,
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({
    CommonModelAutoConfiguration.class,
    TaskModelAutoConfiguration.class,
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
})
@TestPropertySource(
    properties = {
        "activiti.cloud.services.variables.max-variable-value-length=100",
        "activiti.cloud.services.variables.whitelisted-variables=batchState",
    }
)
class VariableValueSizeValidationIT {

    private static final int MAX_VARIABLE_VALUE_LENGTH = 100;
    private static final String TASK_ID = "task-1";

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
    private TaskRuntime taskRuntime;

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

    // --- StartProcessPayload: POST /v1/process-instances ---

    @Test
    void should_rejectStartProcess_when_nonWhitelistedVariablesExceedLimit() throws Exception {
        String largeValue = "x".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("bigVar", largeValue)
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_allowStartProcess_when_variablesWithinLimit() throws Exception {
        when(processRuntime.start(any(StartProcessPayload.class))).thenReturn(defaultProcessInstance());

        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("smallVar", "ok")
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isOk());
    }

    @Test
    void should_allowStartProcess_when_whitelistedVariableExceedsLimit() throws Exception {
        when(processRuntime.start(any(StartProcessPayload.class))).thenReturn(defaultProcessInstance());

        String hugeValue = "x".repeat(1000);
        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("batchState", hugeValue)
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isOk());
    }

    // --- StartProcessPayload: POST /v1/process-instances/{id}/start ---

    @Test
    void should_rejectStartCreatedProcess_when_nonWhitelistedVariablesExceedLimit() throws Exception {
        String largeValue = "x".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("bigVar", largeValue)
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances/{processInstanceId}/start", "123")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_allowStartCreatedProcess_when_variablesWithinLimit() throws Exception {
        when(processRuntime.startCreatedProcess(any(), any(StartProcessPayload.class))).thenReturn(
            defaultProcessInstance()
        );

        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("smallVar", "ok")
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances/{processInstanceId}/start", "123")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isOk());
    }

    @Test
    void should_allowStartCreatedProcess_when_whitelistedVariableExceedsLimit() throws Exception {
        when(processRuntime.startCreatedProcess(any(), any(StartProcessPayload.class))).thenReturn(
            defaultProcessInstance()
        );

        String hugeValue = "x".repeat(1000);
        StartProcessPayload cmd = ProcessPayloadBuilder.start()
            .withProcessDefinitionId("1")
            .withVariable("batchState", hugeValue)
            .build();

        mockMvc
            .perform(
                post("/v1/process-instances/{processInstanceId}/start", "123")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd))
            )
            .andExpect(status().isOk());
    }

    // --- SetProcessVariablesPayload: PUT /v1/process-instances/{id}/variables ---

    @Test
    void should_rejectSetVariables_when_nonWhitelistedVariablesExceedLimit() throws Exception {
        String largeValue = "y".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        Map<String, Object> variables = new HashMap<>();
        variables.put("oversizedVar", largeValue);

        mockMvc
            .perform(
                put("/v1/process-instances/{processInstanceId}/variables", UUID.randomUUID().toString())
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
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_allowSetVariables_when_whitelistedLargeAndNonWhitelistedSmall() throws Exception {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");
        processInstance.setAppVersion("1");
        when(processRuntime.processInstance(any())).thenReturn(processInstance);

        String hugeValue = "x".repeat(1000);
        Map<String, Object> variables = new HashMap<>();
        variables.put("batchState", hugeValue);
        variables.put("smallVar", "ok");

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

    @Test
    void should_rejectSetVariables_when_combinedNonWhitelistedExceedsLimit() throws Exception {
        // Each variable individually within limit, but combined they exceed it
        String value = "z".repeat(60);

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", value);
        variables.put("var2", value);

        mockMvc
            .perform(
                put("/v1/process-instances/{processInstanceId}/variables", UUID.randomUUID().toString())
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
            .andExpect(status().isBadRequest());
    }

    // --- UpdateTaskVariablePayload: PUT {base}/tasks/{taskId}/variables/{variableName} ---
    // The value exceeds the 100-byte variable limit but the request body stays far below
    // the total request-size limit, so the advice (not the filter) performs the validation.

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "/v1", "/admin/v1" })
    void should_rejectUpdateTaskVariable_when_nonWhitelistedValueExceedsLimit(String basePath) throws Exception {
        String oversizedValue = "x".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        mockMvc
            .perform(updateTaskVariable(basePath, "ordinaryVar", "ordinaryVar", oversizedValue))
            .andExpect(status().isBadRequest())
            .andExpect(result ->
                assertThat(result.getResolvedException()).isInstanceOf(VariableValueTooLargeException.class)
            );

        verifyNoTaskVariableUpdate();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "/v1", "/admin/v1" })
    void should_rejectUpdateTaskVariable_when_bodyNameIsWhitelistedButUriNameIsNot(String basePath) throws Exception {
        String oversizedValue = "x".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        // Body claims the whitelisted name; URI targets a non-whitelisted variable.
        mockMvc
            .perform(updateTaskVariable(basePath, "ordinaryVar", "batchState", oversizedValue))
            .andExpect(status().isBadRequest())
            .andExpect(result ->
                assertThat(result.getResolvedException()).isInstanceOf(VariableValueTooLargeException.class)
            );

        verifyNoTaskVariableUpdate();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "/v1", "/admin/v1" })
    void should_allowUpdateTaskVariable_when_uriNameIsWhitelistedRegardlessOfBodyName(String basePath)
        throws Exception {
        String oversizedValue = "x".repeat(MAX_VARIABLE_VALUE_LENGTH + 1);

        // URI targets the whitelisted variable; body name must not cause a rejection.
        mockMvc
            .perform(updateTaskVariable(basePath, "batchState", "ordinaryVar", oversizedValue))
            .andExpect(status().isOk());

        // The controller overwrites the payload name with the URI name before calling the runtime.
        if (basePath.startsWith("/admin")) {
            verify(taskAdminRuntime).updateVariable(argThat(p -> "batchState".equals(p.getName())));
        } else {
            verify(taskRuntime).updateVariable(argThat(p -> "batchState".equals(p.getName())));
        }
    }

    private MockHttpServletRequestBuilder updateTaskVariable(
        String basePath,
        String uriVariableName,
        String payloadVariableName,
        String value
    ) {
        UpdateTaskVariablePayload payload = TaskPayloadBuilder.updateVariable()
            .withTaskId(TASK_ID)
            .withVariable(payloadVariableName, value)
            .build();

        return put(basePath + "/tasks/{taskId}/variables/{variableName}", TASK_ID, uriVariableName)
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(payload));
    }

    private void verifyNoTaskVariableUpdate() {
        verify(taskRuntime, never()).updateVariable(any(UpdateTaskVariablePayload.class));
        verify(taskAdminRuntime, never()).updateVariable(any(UpdateTaskVariablePayload.class));
    }
}
