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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.converter.ProcessDefinitionConverter;
import org.activiti.cloud.services.core.pageable.PageableRepositoryService;
import org.activiti.cloud.services.rest.api.ProcessDefinitionMetaController;
import org.activiti.cloud.services.rest.api.resources.assembler.ProcessDefinitionResourceAssembler;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessDefinitionControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessDefinitionControllerImplTest {

    private static final String DOCUMENTATION_IDENTIFIER = "process-definition";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;
    @MockBean
    private ProcessDiagramGenerator processDiagramGenerator;
    @MockBean
    private ProcessDefinitionConverter processDefinitionConverter;
    @MockBean
    private PageableRepositoryService pageableRepositoryService;
    @MockBean
    private ProcessDefinitionResourceAssembler resourceAssembler;
    @MockBean
    private ProcessDefinitionMetaController processDefinitionMetaController;

    @Test
    public void getProcessDefinitions() throws Exception {
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        Page<ProcessDefinition> processDefinitions = new PageImpl<>(processDefinitionList,
                                                                    PageRequest.of(0,
                                                                                   10),
                                                                    processDefinitionList.size());
        when(pageableRepositoryService.getProcessDefinitions(any())).thenReturn(processDefinitions);

        this.mockMvc.perform(get("/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))

                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("links").description("The hypermedia links."),
                                               subsectionWithPath("content").description("The process definitions."))));
    }

    @Test
    public void getProcessDefinition() throws Exception {
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(new ProcessDefinitionEntityImpl());

        this.mockMvc.perform(get("/v1/process-definitions/{id}",
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("id").description("The process definition id"))));
    }

    @Test
    public void getProcessModel() throws Exception {
        InputStream xml = new ByteArrayInputStream("activiti".getBytes());
        when(repositoryService.getProcessModel("1")).thenReturn(xml);

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).contentType(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/model/get",
                                pathParameters(parameterWithName("id").description("The process model id"))));
    }

    @Test
    public void getBpmnModel() throws Exception {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("1");
        process.setName("Main Process");
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel("1")).thenReturn(bpmnModel);

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/bpmn-model/get",
                                pathParameters(parameterWithName("id").description("The BPMN model id"))));
    }

    @Test
    public void getProcessDiagram() throws Exception {
        BpmnModel bpmnModel = new BpmnModel();
        when(repositoryService.getBpmnModel("1")).thenReturn(bpmnModel);
        InputStream img = new ByteArrayInputStream("img".getBytes());
        when(processDiagramGenerator.generateDiagram(bpmnModel,
                                                     processDiagramGenerator.getDefaultActivityFontName(),
                                                     processDiagramGenerator.getDefaultLabelFontName(),
                                                     processDiagramGenerator.getDefaultAnnotationFontName()))
                .thenReturn(img);

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).contentType("image/svg+xml"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                pathParameters(parameterWithName("id").description("The BPMN model id"))));
    }
}
