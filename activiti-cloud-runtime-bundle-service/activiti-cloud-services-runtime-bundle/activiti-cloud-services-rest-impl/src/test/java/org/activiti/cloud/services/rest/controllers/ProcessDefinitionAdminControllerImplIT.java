/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.ProcessDefinitionsSyncService;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
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
import org.activiti.engine.TaskService;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.activiti.spring.process.model.ConstantDefinition;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessConstantsMapping;
import org.activiti.spring.process.model.VariableDefinition;
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
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ProcessDefinitionAdminControllerImpl.class)
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
    }
)
class ProcessDefinitionAdminControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    @MockitoBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockitoBean
    private TaskAdminRuntime taskAdminRuntime;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean(name = ProcessEngineChannels.COMMAND_RESULTS)
    private MessageChannel commandResults;

    @MockitoBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockitoBean
    private ProcessRuntime processRuntime;

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

    @MockitoBean
    private ProcessExtensionService processExtensionService;

    @BeforeEach
    void setUp() {
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
        assertThat(processRuntime).isNotNull();
    }

    @Test
    void getProcessDefinitions() throws Exception {
        String procId = "procId";
        String processName = "my process";
        String processDescription = "this is my process";
        int version = 1;
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(buildProcessDefinition(procId, processName, processDescription, version));
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(
            processDefinitionList,
            processDefinitionList.size()
        );
        when(processAdminRuntime.processDefinitions(any(Pageable.class), isNull())).thenReturn(processDefinitionPage);

        this.mockMvc.perform(get("/admin/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    void getProcessDefinitionsShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        String processDefId = UUID.randomUUID().toString();
        ProcessDefinition processDefinition = buildProcessDefinition(
            processDefId,
            "my process",
            "This is my process",
            1
        );

        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList, 11);

        given(processAdminRuntime.processDefinitions(any(Pageable.class), isNull())).willReturn(processDefinitionPage);

        //when
        MvcResult result =
            this.mockMvc.perform(
                    get("/admin/v1/process-definitions?skipCount=10&maxItems=10")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andReturn();

        //then
        String responseContent = result.getResponse().getContentAsString();
        assertThatJson(responseContent).inPath("list.pagination.skipCount").isEqualTo(10);
        assertThatJson(responseContent).inPath("list.pagination.maxItems").isEqualTo(10);
        assertThatJson(responseContent).inPath("list.pagination.count").isEqualTo(1);
        assertThatJson(responseContent).inPath("list.pagination.hasMoreItems").isEqualTo(false);
        assertThatJson(responseContent).inPath("list.pagination.totalItems").isEqualTo(11);
        assertThatJson(responseContent).inPath("list.entries[0].entry.id").isEqualTo(processDefId);
        assertThatJson(responseContent).inPath("list.entries[0].entry.name").isEqualTo("my process");
        assertThatJson(responseContent).inPath("list.entries[0].entry.description").isEqualTo("This is my process");
        assertThatJson(responseContent).inPath("list.entries[0].entry.version").isEqualTo(1);
    }

    @Test
    void should_getProcessDefinitionsWithVariables() throws Exception {
        var procId = "procId";
        var processName = "my process";
        var processDescription = "this is my process";
        int version = 1;
        var processDefinition = buildProcessDefinition(procId, processName, processDescription, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(
            processDefinitionList,
            processDefinitionList.size()
        );
        when(processAdminRuntime.processDefinitions(any(Pageable.class), isNull())).thenReturn(processDefinitionPage);

        var extension = new Extension();
        var givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("VAR_ID");
        givenVariableDefinition.setName("var1");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");
        extension.setProperties(Map.of("var1", givenVariableDefinition));

        when(processExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        mockMvc
            .perform(
                get("/admin/v1/process-definitions")
                    .queryParam("include", "variables")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.variableDefinitions[0].displayName").value("Var name"));
    }

    @Test
    void should_getProcessDefinitionsWithConstantValues() throws Exception {
        var procId = "procId";
        var processName = "my process";
        var processDescription = "this is my process";
        int version = 1;
        var startEventId = "start_event";
        var processDefinition = buildProcessDefinition(procId, processName, processDescription, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(
            processDefinitionList,
            processDefinitionList.size()
        );
        when(processAdminRuntime.processDefinitions(any(Pageable.class), isNull())).thenReturn(processDefinitionPage);

        var extension = new Extension();
        var processConstantMapping = new ProcessConstantsMapping();
        var constantDefinition = new ConstantDefinition();
        constantDefinition.setValue(true);
        processConstantMapping.put("unauthorizedStart", constantDefinition);
        extension.setConstants(Map.of(startEventId, processConstantMapping));

        when(processExtensionService.getExtensionsForId("procId")).thenReturn(extension);
        var bpmnModel = mock(BpmnModel.class);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);
        var process = new Process();
        var startEvent = new StartEvent();
        startEvent.setId(startEventId);
        startEvent.setFormKey("formKey");
        process.setInitialFlowElement(startEvent);
        process.addFlowElement(startEvent);
        when(bpmnModel.getProcessById(any())).thenReturn(process);
        mockMvc
            .perform(
                get("/admin/v1/process-definitions")
                    .queryParam("include", "constant-values")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list.entries[0].entry.constantValues.unauthorizedStart").value(true));
    }

    @Test
    void getProcessDefinitionsExcludingProcessTriggerableByFormCategory() throws Exception {
        String procId = "procId";
        String processName = "my process";
        String processDescription = "this is my process";
        int version = 1;
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(buildProcessDefinition(procId, processName, processDescription, version));
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(
            processDefinitionList,
            processDefinitionList.size()
        );
        when(processAdminRuntime.processDefinitions(any(Pageable.class), any(GetProcessDefinitionsPayload.class)))
            .thenReturn(processDefinitionPage);

        this.mockMvc.perform(
                get("/admin/v1/process-definitions")
                    .queryParam("excludedCategory", "#triggerableByForm")
                    .accept(MediaTypes.HAL_JSON_VALUE)
            )
            .andExpect(status().isOk());
    }

    private ProcessDefinition buildProcessDefinition(
        String processDefinitionId,
        String name,
        String description,
        int version
    ) {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefinitionId);
        processDefinition.setName(name);
        processDefinition.setDescription(description);
        processDefinition.setVersion(version);
        return processDefinition;
    }
}
