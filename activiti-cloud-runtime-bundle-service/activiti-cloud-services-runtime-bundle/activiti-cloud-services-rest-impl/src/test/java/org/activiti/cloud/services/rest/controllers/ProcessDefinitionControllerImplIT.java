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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.CachingProcessExtensionService;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.Mapping;
import org.activiti.spring.process.model.Mapping.SourceMappingType;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.activiti.spring.process.model.VariableDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessDefinitionControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        ServicesCoreAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class ProcessDefinitionControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @MockBean
    private StreamBridge streamBridge;

    @MockBean
    private ProcessRuntime processRuntime;

    @MockBean
    private TaskAdminRuntime taskAdminRuntime;

    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private MessageChannel commandResults;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockBean
    private CachingProcessExtensionService cachingProcessExtensionService;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    public void getProcessDefinitions() throws Exception {

        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(buildProcessDefinition(procId, my_process, this_is_my_process, version));
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        mockMvc.perform(get("/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk());
    }

    private ProcessDefinition buildProcessDefinition(String processDefinitionId,
                                                     String name,
                                                     String description,
                                                     int version) {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefinitionId);
        processDefinition.setName(name);
        processDefinition.setDescription(description);
        processDefinition.setVersion(version);
        return processDefinition;
    }

    @Test
    public void getProcessDefinitionsShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        String processDefId = UUID.randomUUID().toString();
        ProcessDefinition processDefinition = buildProcessDefinition(processDefId,
                                                                     "my process",
                                                                     "This is my process",
                                                                     1);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                                                                       11);
        given(processRuntime.processDefinitions(any())).willReturn(processDefinitionPage);

        //when
        MvcResult result = mockMvc.perform(get("/v1/process-definitions?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String responseContent = result.getResponse().getContentAsString();
        assertThatJson(responseContent)
                .node("list.pagination.skipCount").isEqualTo(10)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(11);
        assertThatJson(responseContent)
                .node("list.entries[0].entry.id").isEqualTo(processDefId)
                .node("list.entries[0].entry.name").isEqualTo("my process")
                .node("list.entries[0].entry.description").isEqualTo("This is my process")
                .node("list.entries[0].entry.version").isEqualTo(1);
    }

    @Test
    public void getProcessDefinitionsWithVariables() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        Extension extension = new Extension();
        VariableDefinition givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("VAR_ID");
        givenVariableDefinition.setName("var1");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");
        extension.setProperties(Map.of("var1", givenVariableDefinition));

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        mockMvc.perform(get("/v1/process-definitions")
                .queryParam("include", "variables")
                .accept(MediaTypes.HAL_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.processDefinitions[0].variableDefinitions[0].displayName")
                .value("Var name"));
    }

    @Test
    public void shouldGetProcessDefinitionById() throws Exception {
        //given
        String processId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processId))
                .willReturn(buildProcessDefinition(processId,
                                                   "my process",
                                                   "this is my process",
                                                   1));

        mockMvc.perform(get("/v1/process-definitions/{id}",
                                 processId).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    public void getProcessDefinitionShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        String procDefId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(procDefId))
                .willReturn(buildProcessDefinition(procDefId,
                                                   "my process",
                                                   "This is my process",
                                                   1));

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}",
                                                    procDefId).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("entry.id").isEqualTo(procDefId)
                .node("entry.name").isEqualTo("my process")
                .node("entry.description").isEqualTo("This is my process")
                .node("entry.version").isEqualTo(1)
        ;
    }

    @Test
    public void shouldGetXMLProcessModel() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processDefinitionId))
                .willReturn(mock(ProcessDefinition.class));

        InputStream xml = new ByteArrayInputStream("activiti".getBytes());
        when(repositoryService.getProcessModel(processDefinitionId)).thenReturn(xml);

        mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetBpmnJsonModel() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processDefinitionId))
                .willReturn(mock(ProcessDefinition.class));

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(processDefinitionId);
        process.setName("Main Process");
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(processDefinitionId)).thenReturn(bpmnModel);

        mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetSVGProcessDiagram() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processDefinitionId))
                .willReturn(mock(ProcessDefinition.class));

        BpmnModel bpmnModel = new BpmnModel();
        when(repositoryService.getBpmnModel(processDefinitionId)).thenReturn(bpmnModel);
        when(processDiagramGenerator.generateDiagram(any(BpmnModel.class)))
                .thenReturn("img".getBytes());

        mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept("image/svg+xml"))
                .andExpect(status().isOk());
    }

    @Test
    public void should_getProcessDiagramReturnNotFound_when_processDefinitionIsNotFound() throws Exception {
        String processDefinitionId = "missingProcessDefinitionId";
        willThrow(new ActivitiObjectNotFoundException("not found"))
            .given(processRuntime).processDefinition(processDefinitionId);

        mockMvc.perform(get("/v1/process-definitions/{id}/model", processDefinitionId)
            .accept("image/svg+xml"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("entry.code", is(404)))
            .andExpect(jsonPath("entry.message", is("not found")));
    }

    @Test
    public void should_getProcessDiagramReturnNoContent_when_noInterchangeInfo() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        BpmnModel bpmnModel = new BpmnModel();
        given(repositoryService.getBpmnModel(processDefinitionId)).willReturn(bpmnModel);

        willThrow(new ActivitiInterchangeInfoNotFoundException("No interchange information found."))
            .given(processDiagramGenerator).generateDiagram(bpmnModel);

        mockMvc.perform(get("/v1/process-definitions/{id}/model", processDefinitionId)
            .accept("image/svg+xml"))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("entry.code", is(404)))
            .andExpect(jsonPath("entry.message", is("No interchange information found.")));
        verify(processRuntime).processDefinition(processDefinitionId);
    }

    @Test
    public void should_getProcessModelStaticValuesMappingForStartEvent_when_hasStartEventFormAndMappings() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(procId);
        process.setName("Main Process");
        StartEvent startEvent = new StartEvent();
        startEvent.setName("startEvent");
        startEvent.setId("startEvent");
        startEvent.setFormKey("start-event-form");
        process.setInitialFlowElement(startEvent);
        process.addFlowElement(startEvent);
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);


        ProcessVariablesMapping startEventMapping = new ProcessVariablesMapping();
        Mapping valueMapping = new Mapping();
        valueMapping.setValue("static value");
        valueMapping.setType(SourceMappingType.VALUE);
        Mapping variableMapping = new Mapping();
        variableMapping.setValue("variableRef");
        variableMapping.setType(SourceMappingType.VARIABLE);
        startEventMapping.setInputs(Map.of("value",valueMapping,"variable",variableMapping));

        VariableDefinition givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("variableRef");
        givenVariableDefinition.setName("variableRef");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");

        Extension extension = new Extension();
        extension.setMappings(Map.of("startEvent", startEventMapping));
        extension.setProperties(Map.of("variableRef", givenVariableDefinition));

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}/static-values",procId)
                .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> resultMap = om.readValue(result.getResponse().getContentAsString(), Map.class);

        assertThat(resultMap).isEqualTo(Map.of("value", "static value"));
    }

    @Test
    public void should_getEmptyMapForProcessModelStaticValuesMappingForStartEvent_when_startEventHasNoFormAndMappings() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(procId);
        process.setName("Main Process");
        StartEvent startEvent = new StartEvent();
        startEvent.setName("startEvent");
        startEvent.setId("startEvent");
        process.setInitialFlowElement(startEvent);
        process.addFlowElement(startEvent);
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);

        ProcessVariablesMapping startEventMapping = new ProcessVariablesMapping();
        Mapping valueMapping = new Mapping();
        valueMapping.setValue("static value");
        valueMapping.setType(SourceMappingType.VALUE);
        Mapping variableMapping = new Mapping();
        variableMapping.setValue("variableRef");
        variableMapping.setType(SourceMappingType.VARIABLE);
        startEventMapping.setInputs(Map.of("value",valueMapping,"variable",variableMapping));

        VariableDefinition givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("variableRef");
        givenVariableDefinition.setName("variableRef");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");

        Extension extension = new Extension();
        extension.setMappings(Map.of("startEvent", startEventMapping));
        extension.setProperties(Map.of("variableRef", givenVariableDefinition));

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}/static-values",procId)
                .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> resultMap = om.readValue(result.getResponse().getContentAsString(), Map.class);

        assertThat(resultMap).isEqualTo(Map.of());
    }

    @Test
    public void should_getEmptyMapForProcessModelStaticValuesMappingForStartEvent_when_hasNoMappingForStartEvent() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(procId);
        process.setName("Main Process");
        StartEvent startEvent = new StartEvent();
        startEvent.setName("startEvent");
        startEvent.setId("startEvent");
        startEvent.setFormKey("start-event-form");
        process.setInitialFlowElement(startEvent);
        process.addFlowElement(startEvent);
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);

        VariableDefinition givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("variableRef");
        givenVariableDefinition.setName("variableRef");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");

        Extension extension = new Extension();
        extension.setProperties(Map.of("variableRef", givenVariableDefinition));

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}/static-values",procId)
                .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> resultMap = om.readValue(result.getResponse().getContentAsString(), Map.class);

        assertThat(resultMap).isEqualTo(Map.of());
    }

    @Test
    public void should_getEmptyMapForProcessModelStaticValuesMappingForStartEvent_when_hasNoStartEvent() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(procId);
        process.setName("Main Process");
        StartEvent startEvent = new StartEvent();
        startEvent.setName("startEvent");
        startEvent.setId("startEvent");
        startEvent.setFormKey("start-event-form");
        process.setInitialFlowElement(startEvent);
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);

        ProcessVariablesMapping startEventMapping = new ProcessVariablesMapping();
        Mapping valueMapping = new Mapping();
        valueMapping.setValue("static value");
        valueMapping.setType(SourceMappingType.VALUE);
        Mapping variableMapping = new Mapping();
        variableMapping.setValue("variableRef");
        variableMapping.setType(SourceMappingType.VARIABLE);
        startEventMapping.setInputs(Map.of("value",valueMapping,"variable",variableMapping));

        VariableDefinition givenVariableDefinition = new VariableDefinition();
        givenVariableDefinition.setId("variableRef");
        givenVariableDefinition.setName("variableRef");
        givenVariableDefinition.setDescription("Variable no 1");
        givenVariableDefinition.setType("string");
        givenVariableDefinition.setRequired(true);
        givenVariableDefinition.setDisplay(true);
        givenVariableDefinition.setDisplayName("Var name");

        Extension extension = new Extension();
        extension.setMappings(Map.of("startEvent", startEventMapping));
        extension.setProperties(Map.of("variableRef", givenVariableDefinition));

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(extension);

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}/static-values",procId)
                .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> resultMap = om.readValue(result.getResponse().getContentAsString(), Map.class);

        assertThat(resultMap).isEqualTo(Map.of());
    }

    @Test
    public void should_getEmptyMapForProcessModelStaticValuesMappingForStartEvent_when_hasNoExtensions() throws Exception {
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        ProcessDefinition processDefinition = buildProcessDefinition(procId, my_process, this_is_my_process, version);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        processDefinitionList.add(processDefinition);
        Page<ProcessDefinition> processDefinitionPage =
            new PageImpl<>(processDefinitionList, processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(procId);
        process.setName("Main Process");
        StartEvent startEvent = new StartEvent();
        startEvent.setName("startEvent");
        startEvent.setId("startEvent");
        startEvent.setFormKey("start-event-form");
        process.setInitialFlowElement(startEvent);
        process.addFlowElement(startEvent);
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel(procId)).thenReturn(bpmnModel);

        when(cachingProcessExtensionService.getExtensionsForId("procId")).thenReturn(null);

        MvcResult result = mockMvc.perform(get("/v1/process-definitions/{id}/static-values",procId)
                .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> resultMap = om.readValue(result.getResponse().getContentAsString(), Map.class);

        assertThat(resultMap).isEqualTo(Map.of());
    }
}
