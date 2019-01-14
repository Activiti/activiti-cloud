/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.rest.controllers;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processDefinitionFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processDefinitionIdParameter;
import static org.activiti.alfresco.rest.docs.HALDocumentation.pagedProcessDefinitionFields;
import static org.activiti.alfresco.rest.docs.HALDocumentation.selfLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ProcessDefinitionControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class,
        ServicesCoreAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class ProcessDefinitionControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-definition";
   
    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-definition-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private ProcessRuntime processRuntime;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getProcessDefinitions() throws Exception {

        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        List<ProcessDefinition> processDefinitionList = Collections.singletonList(buildProcessDefinition(procId,
                                                                                                         my_process,
                                                                                                         this_is_my_process,
                                                                                                         version));
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                                                                       processDefinitionList.size());
        when(processRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        this.mockMvc.perform(get("/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))
        .andDo(print())
        .andExpect(status().isOk())
        .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                        pagedProcessDefinitionFields(),
                        links(selfLink()
                       )
        ));
        
        
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
        List<ProcessDefinition> processDefinitionList = Collections.singletonList(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                                                                       11);
        given(processRuntime.processDefinitions(any())).willReturn(processDefinitionPage);

        //when
        MvcResult result = this.mockMvc.perform(get("/v1/process-definitions?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()))
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
    public void shouldGetProcessDefinitionById() throws Exception {
        //given
        String processId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processId))
                .willReturn(buildProcessDefinition(processId,
                                                   "my process",
                                                   "this is my process",
                                                   1));

        this.mockMvc.perform(get("/v1/process-definitions/{id}",
                                 processId).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                processDefinitionIdParameter()));
    }

    @Test
    public void getProcessDefinitionShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        String procDefId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(procDefId))
                .willReturn(buildProcessDefinition(procDefId,
                                                   "my process",
                                                   "This is my process",
                                                   1));

        MvcResult result = this.mockMvc.perform(get("/v1/process-definitions/{id}",
                                                    procDefId).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/get",
                                processDefinitionIdParameter(),
                                processDefinitionFields()
                       )
                ).andReturn();

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

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/model/get",
                                processDefinitionIdParameter()));
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

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/bpmn-model/get",
                                processDefinitionIdParameter()));
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

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept("image/svg+xml"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                processDefinitionIdParameter()));
    }

    @Test
    public void getProcessDiagramShouldReturnNotFoundWhenRelatedProcessDefinitionIsNotFound() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processDefinitionId))
                .willThrow(new ActivitiObjectNotFoundException("missing"));

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    processDefinitionId).accept("image/svg+xml"))
                .andExpect(status().isNotFound())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                processDefinitionIdParameter()));
    }

    @Test
    public void getProcessDiagramShouldReturnNoContentStatusWhenNoInterchangeInfo() throws Exception {
        String processDefinitionId = UUID.randomUUID().toString();
        given(processRuntime.processDefinition(processDefinitionId))
                .willReturn(mock(ProcessDefinition.class));

        BpmnModel bpmnModel = new BpmnModel();
        when(repositoryService.getBpmnModel("1")).thenReturn(bpmnModel);
        when(processDiagramGenerator.generateDiagram(any(BpmnModel.class)))
                .thenThrow(new ActivitiInterchangeInfoNotFoundException("No interchange information found."));

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).accept("image/svg+xml"))
                .andExpect(status().isNoContent());
    }
}
