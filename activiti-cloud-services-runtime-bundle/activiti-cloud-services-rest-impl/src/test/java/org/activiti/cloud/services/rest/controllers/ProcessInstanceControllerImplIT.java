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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.engine.RepositoryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-instance-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityPoliciesApplicationService securityService;
    @MockBean
    private ProcessEngineWrapper processEngine;
    @MockBean
    private RepositoryService repositoryService;
    @MockBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;
    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void getProcessInstances() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(buildDefaultProcessInstance());
        Page<ProcessInstance> processInstances = new PageImpl<>(processInstanceList,
                                                                PageRequest.of(0,
                                                                               10),
                                                                processInstanceList.size());
        when(processEngine.getProcessInstances(any())).thenReturn(processInstances);

        this.mockMvc.perform(get("/v1/process-instances"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions."))));
    }

    @Test
    public void getProcessInstancesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(buildDefaultProcessInstance());
        Page<ProcessInstance> processInstancePage = new PageImpl<>(processInstanceList,
                                                                   PageRequest.of(1,
                                                                                  10),
                                                                   processInstanceList.size());
        when(processEngine.getProcessInstances(any())).thenReturn(processInstancePage);

        this.mockMvc.perform(get("/v1/process-instances?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }

    private ProcessInstance buildDefaultProcessInstance() {
        return new ProcessInstance(UUID.randomUUID().toString(),
                                   "My process instance",
                                   "This is my process instance",
                                   UUID.randomUUID().toString(),
                                   "user",
                                   new Date(),
                                   "my business key",
                                   ProcessInstance.ProcessInstanceStatus.RUNNING.name(),
                                   "my-proc-def");
    }

    @Test
    public void startProcess() throws Exception {
        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd("1");

        when(processEngine.startProcess(any())).thenReturn(buildDefaultProcessInstance());

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
        ProcessInstance processInstance = buildDefaultProcessInstance();
        when(processEngine.getProcessInstanceById("1")).thenReturn(processInstance);
        when(securityService.canRead(processInstance.getProcessDefinitionKey())).thenReturn(true);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void getProcessInstanceByIdNotFound() throws Exception {
        when(processEngine.getProcessInstanceById("1")).thenReturn(null);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProcessDiagram() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processEngine.getProcessInstanceById(anyString())).thenReturn(processInstance);
        when(repositoryService.getBpmnModel(processInstance.getProcessDefinitionId())).thenReturn(mock(BpmnModel.class));
        when(securityService.canRead(processInstance.getProcessDefinitionId())).thenReturn(true);
        when(processEngine.getActiveActivityIds(anyString())).thenReturn(Collections.emptyList());

        when(processDiagramGenerator.generateDiagram(any(BpmnModel.class),
                                                     anyList(),
                                                     anyList()))
                .thenReturn("diagram".getBytes());

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 1).contentType("image/svg+xml"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void getProcessDiagramProcessNotFound() throws Exception {
        when(processEngine.getProcessInstanceById(anyString())).thenReturn(null);
        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 1).contentType("image/svg+xml"))
                .andExpect(status().isNotFound());
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
