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
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.activiti.runtime.api.NotFoundException;
import org.activiti.runtime.api.cmd.impl.SendSignalImpl;
import org.activiti.runtime.api.cmd.impl.StartProcessImpl;
import org.activiti.runtime.api.model.FluentProcessInstance;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.conf.CommonModelAutoConfiguration;
import org.conf.activiti.runtime.ProcessModelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.cloud.services.rest.controllers.ProcessInstanceSamples.defaultProcessInstance;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ProcessInstanceControllerImpl.class, secure = false)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-instance-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void getProcessInstances() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstances = new PageImpl<>(processInstanceList,
                                                                PageRequest.of(0,
                                                                               10),
                                                                processInstanceList.size());
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstances(any())).thenReturn(processInstances);

        this.mockMvc.perform(get("/v1/process-instances"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions."))));
    }

    @Test
    public void getProcessInstancesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstancePage = new PageImpl<>(processInstanceList,
                                                                   PageRequest.of(1,
                                                                                  10),
                                                                   processInstanceList.size());
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstances(any())).thenReturn(processInstancePage);

        this.mockMvc.perform(get("/v1/process-instances?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }

    @Test
    public void startProcess() throws Exception {
        StartProcessImpl cmd = new StartProcessImpl("1");

        when(securityAwareProcessInstanceService.startProcess(any())).thenReturn(defaultProcessInstance());

        this.mockMvc.perform(post("/v1/process-instances")
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/start"));
    }

    @Test
    public void startProcessForbidden() throws Exception {
        StartProcessImpl cmd = new StartProcessImpl("1");

        when(securityAwareProcessInstanceService.startProcess(any())).thenThrow(new ActivitiForbiddenException("Not permitted"));

        this.mockMvc.perform(post("/v1/process-instances")
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/start"));
    }

    @Test
    public void getProcessInstanceById() throws Exception {
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById("1")).thenReturn(defaultProcessInstance());

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void getProcessInstanceByIdShouldReturnNotFoundStatusWhenServiceThrowsNotFoundException() throws Exception {
        String processInstanceId = UUID.randomUUID().toString();
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstanceId))
                .thenThrow(new NotFoundException("not found"));

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 processInstanceId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProcessDiagram() throws Exception {
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(anyString())).thenReturn(processInstance);
        when(repositoryService.getBpmnModel(processInstance.getProcessDefinitionId())).thenReturn(mock(BpmnModel.class));
        when(processInstance.activeActivityIds()).thenReturn(Collections.emptyList());

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
    public void getProcessDiagramShouldReturnNotFoundStatusWhenServiceThrowsNotFoundException() throws Exception {
        String processInstanceId = UUID.randomUUID().toString();
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstanceId))
                .thenThrow(new NotFoundException("not found"));
        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 processInstanceId).contentType("image/svg+xml"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProcessDiagramWithoutInterchangeInfo() throws Exception {
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(anyString())).thenReturn(processInstance);
        when(repositoryService.getBpmnModel(processInstance.getProcessDefinitionId())).thenReturn(mock(BpmnModel.class));
        when(processInstance.activeActivityIds()).thenReturn(Collections.emptyList());

        when(processDiagramGenerator.generateDiagram(any(BpmnModel.class),
                                                     anyList(),
                                                     anyList()))
                .thenThrow(new ActivitiInterchangeInfoNotFoundException("No interchange information found."));

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 1).contentType("image/svg+xml"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void sendSignal() throws Exception {
        SendSignalImpl cmd = new SendSignalImpl("signalInstance");

        this.mockMvc.perform(get("/v1/process-instances/signal").contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/signal"));
    }

    @Test
    public void suspend() throws Exception {
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById("1")).thenReturn(processInstance);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/suspend",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/suspend",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void activate() throws Exception {
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById("1")).thenReturn(processInstance);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/activate",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/activate",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }


    @Test
    public void deleteProcessInstance() throws Exception {
        FluentProcessInstance processInstance = mock(FluentProcessInstance.class);
        when(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById("1")).thenReturn(processInstance);

        this.mockMvc.perform(delete("/v1/process-instances/{processInstanceId}", 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/delete",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }
}
