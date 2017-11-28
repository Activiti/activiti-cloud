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
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.rest.api.resources.assembler.ProcessInstanceResourceAssembler;
import org.activiti.engine.RepositoryService;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessInstanceControllerImplTest {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;
    @MockBean
    private ProcessEngineWrapper processEngine;
    @MockBean
    private RepositoryService repositoryService;
    @MockBean
    private ProcessDiagramGenerator processDiagramGenerator;
    @MockBean
    private ProcessInstanceResourceAssembler resourceAssembler;
    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void getProcessInstances() throws Exception {

        List<ProcessInstance> processInstanceList = new ArrayList<>();
        Page<ProcessInstance> processInstances = new PageImpl<>(processInstanceList,
                                                                PageRequest.of(0,
                                                                               10),
                                                                processInstanceList.size());
        when(processEngine.getProcessInstances(any())).thenReturn(processInstances);

        this.mockMvc.perform(get("/v1/process-instances"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("links").description("The hypermedia links."),
                                               subsectionWithPath("content").description("The process definitions."))));
    }

    @Test
    public void startProcess() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd("1");

        when(processEngine.startProcess(cmd)).thenReturn(processInstance);

        this.mockMvc.perform(post("/v1/process-instances")
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/start"));
    }

    @Test
    public void startProcessForbidden() throws Exception {
        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd("1");

        when(processEngine.startProcess(any())).thenThrow(new ActivitiForbiddenException("Not permitted"));

        this.mockMvc.perform(post("/v1/process-instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/start"));
    }

    @Test
    public void getProcessInstanceById() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processEngine.getProcessInstanceById("1")).thenReturn(processInstance);
        when(securityPoliciesApplicationService.canRead(processInstance.getProcessDefinitionId())).thenReturn(true);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }


    @Test
    public void getProcessDiagram() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processEngine.getProcessInstanceById("1")).thenReturn(processInstance);
        InputStream diagram = new ByteArrayInputStream("diagram".getBytes());
        BpmnModel bpmnModel = mock(BpmnModel.class);
        when(repositoryService.getBpmnModel(processInstance.getProcessDefinitionId())).thenReturn(bpmnModel);
        when(securityPoliciesApplicationService.canRead(processInstance.getProcessDefinitionId())).thenReturn(true);
        List<String> activitiIds = new ArrayList<>();
        when(processEngine.getActiveActivityIds("1")).thenReturn(activitiIds);

        when(processDiagramGenerator.generateDiagram(bpmnModel,
                                                     activitiIds,
                                                     Collections.emptyList(),
                                                     processDiagramGenerator.getDefaultActivityFontName(),
                                                     processDiagramGenerator.getDefaultLabelFontName(),
                                                     processDiagramGenerator.getDefaultAnnotationFontName())).thenReturn(diagram);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 1).contentType("image/svg+xml"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void sendSignal() throws Exception {
        SignalProcessInstancesCmd cmd = new SignalProcessInstancesCmd("signalInstance");

        this.mockMvc.perform(get("/v1/process-instances/signal").contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/signal"));
    }

    @Test
    public void suspend() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processEngine.getProcessInstanceById("1")).thenReturn(processInstance);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/suspend",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/suspend",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void activate() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processEngine.getProcessInstanceById("1")).thenReturn(processInstance);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/activate",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/activate",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }
}
