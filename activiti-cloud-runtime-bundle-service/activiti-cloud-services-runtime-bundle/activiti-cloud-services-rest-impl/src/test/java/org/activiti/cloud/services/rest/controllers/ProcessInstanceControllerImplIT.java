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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstanceMeta;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.UnprocessableEntityException;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.activiti.engine.RepositoryService;
import org.activiti.image.exception.ActivitiInterchangeInfoNotFoundException;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.activiti.cloud.services.rest.controllers.ProcessInstanceSamples.defaultProcessInstance;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessInstanceControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        ServicesCoreAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
public class ProcessInstanceControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

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

    @Test
    public void getProcessInstances() throws Exception {
        //given
        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstancePage = new PageImpl<>(processInstanceList,
                                                                   processInstanceList.size());

        when(processRuntime.processInstances(any())).thenReturn(processInstancePage);

        //when
        mockMvc.perform(get("/v1/process-instances?page=0&size=10")
                .accept(MediaTypes.HAL_JSON_VALUE))
                //then
                .andExpect(status().isOk());
    }

    @Test
    public void getProcessInstancesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstancePage = new PageImpl<>(processInstanceList,
                                                                   processInstanceList.size());
        when(processRuntime.processInstances(any())).thenReturn(processInstancePage);

        mockMvc.perform(get("/v1/process-instances?skipCount=10&maxItems=10").accept(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void startProcess() throws Exception {
        StartProcessPayload cmd = ProcessPayloadBuilder.start().withProcessDefinitionId("1").build();
        when(processRuntime.start(any(StartProcessPayload.class))).thenReturn(defaultProcessInstance());

        mockMvc.perform(post("/v1/process-instances")
                                     .contentType(APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());
    }

    @Test
    public void createProcess() throws Exception {
        StartProcessPayload cmd = ProcessPayloadBuilder.start().withProcessDefinitionId("1").build();
        when(processRuntime.create(any(StartProcessPayload.class))).thenReturn(defaultProcessInstance());

        mockMvc.perform(post("/v1/process-instances/create")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(cmd)))
            .andExpect(status().isOk());
    }

    @Test
    public void startCreatedProcess() throws Exception {
        when(processRuntime.startCreatedProcess("1")).thenReturn(defaultProcessInstance());

        mockMvc.perform(post("/v1/process-instances/{processInstanceId}/start", 1))
            .andExpect(status().isOk());
    }

    @Test
    public void should_startProcessReturnForbidden_when_activitiForbiddenExceptionIsThrownByTheController() throws Exception {
        StartProcessPayload cmd = ProcessPayloadBuilder.start().withProcessDefinitionId("1").build();

        willThrow(new ActivitiForbiddenException("Not permitted"))
            .given(processRuntime).start(any(StartProcessPayload.class));

        mockMvc.perform(post("/v1/process-instances")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(cmd)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("entry.code", is(403)))
            .andExpect(jsonPath("entry.message", is("Not permitted")));
    }

    @Test
    public void should_startProcessReturnUnprocessableEntity_when_unprocessableEntityExceptionIsThrownByController() throws Exception {
        StartProcessPayload cmd = ProcessPayloadBuilder.start().withProcessDefinitionId("1").build();

        willThrow(new UnprocessableEntityException("Unprocessable entry"))
            .given(processRuntime).start(any(StartProcessPayload.class));

        mockMvc.perform(post("/v1/process-instances")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(cmd)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("entry.code", is(422)))
            .andExpect(jsonPath("entry.message", is("Unprocessable entry")));
    }

    @Test
    public void getProcessInstanceById() throws Exception {
        when(processRuntime.processInstance("1")).thenReturn(defaultProcessInstance());

        mockMvc.perform(get("/v1/process-instances/{processInstanceId}", 1))
                .andExpect(status().isOk());
    }

    @Test
    public void should_getProcessInstanceByIdReturnNotFound_when_notFoundExceptionIsThrownByController() throws Exception {
        String processInstanceId = "nonExistentProcessInstanceId";
        willThrow(new NotFoundException("not found"))
            .given(processRuntime).processInstance(processInstanceId);

        mockMvc.perform(get("/v1/process-instances/{processInstanceId}", processInstanceId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("entry.code", is(404)))
            .andExpect(jsonPath("entry.message", is("not found")));
    }

    @Test
    public void getProcessDiagram() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processRuntime.processInstance(anyString())).thenReturn(processInstance);
        when(repositoryService.getBpmnModel(processInstance.getProcessDefinitionId())).thenReturn(mock(BpmnModel.class));
        ProcessInstanceMeta processInstanceMeta = mock(ProcessInstanceMeta.class);
        when(processRuntime.processInstanceMeta(any())).thenReturn(processInstanceMeta);
        when(processInstanceMeta.getActiveActivitiesIds()).thenReturn(emptyList());

        when(processDiagramGenerator.generateDiagram(any(BpmnModel.class),
                                                     anyList(),
                                                     anyList()))
                .thenReturn("diagram".getBytes());

        mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model",
                                 1).contentType("image/svg+xml"))
                .andExpect(status().isOk());
    }

    @Test
    public void should_getProcessDiagramReturnNotFound_when_notFoundExceptionIsThrownByController() throws Exception {
        String processInstanceId = "nonExistentProcessInstanceId";
        willThrow(new NotFoundException("not found"))
            .given(processRuntime).processInstance(processInstanceId);

        mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model", processInstanceId)
            .contentType("image/svg+xml"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("entry.code", is(404)))
            .andExpect(jsonPath("entry.message", is("not found")));
    }

    @Test
    public void should_getProcessDiagram_when_NoInterchangeInfo() throws Exception {
        String processInstanceId = UUID.randomUUID().toString();
        String processDefinitionId = UUID.randomUUID().toString();
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(processInstanceId);
        processInstance.setProcessDefinitionId(processDefinitionId);
        given(processRuntime.processInstance(processInstanceId)).willReturn(processInstance);
        BpmnModel bpmnModel = new BpmnModel();
        given(repositoryService.getBpmnModel(processDefinitionId)).willReturn(bpmnModel);
        ProcessInstanceMeta processInstanceMeta = mock(ProcessInstanceMeta.class);
        given(processRuntime.processInstanceMeta(processInstanceId)).willReturn(processInstanceMeta);
        given(processInstanceMeta.getActiveActivitiesIds()).willReturn(emptyList());

        willThrow(new ActivitiInterchangeInfoNotFoundException("No interchange information found."))
            .given(processDiagramGenerator).generateDiagram(bpmnModel, emptyList(), emptyList());

        mockMvc.perform(get("/v1/process-instances/{processInstanceId}/model", processInstanceId)
            .contentType("image/svg+xml"))
            .andExpect(status().isNoContent())
            .andExpect(jsonPath("entry.code", is(404)))
            .andExpect(jsonPath("entry.message", is("No interchange information found.")));
    }

    @Test
    public void sendSignal() throws Exception {
        SignalPayload cmd = ProcessPayloadBuilder.signal().withName("signalInstance").build();

        mockMvc.perform(post("/v1/process-instances/signal").contentType(APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());
    }

    @Test
    public void suspend() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processRuntime.processInstance("1")).thenReturn(processInstance);
        when(processRuntime.suspend(any())).thenReturn(defaultProcessInstance());
        mockMvc.perform(post("/v1/process-instances/{processInstanceId}/suspend", 1))
                .andExpect(status().isOk());
    }

    @Test
    public void resume() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processRuntime.processInstance("1")).thenReturn(processInstance);
        when(processRuntime.resume(any())).thenReturn(defaultProcessInstance());
        mockMvc.perform(post("/v1/process-instances/{processInstanceId}/resume",
                                 1))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteProcessInstance() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processRuntime.processInstance("1")).thenReturn(processInstance);
        when(processRuntime.delete(any())).thenReturn(defaultProcessInstance());
        mockMvc.perform(delete("/v1/process-instances/{processInstanceId}",
                                    1))
                .andExpect(status().isOk());
    }

    @Test
    public void update() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processRuntime.processInstance("1")).thenReturn(processInstance);
        when(processRuntime.update(any())).thenReturn(defaultProcessInstance());

        UpdateProcessPayload cmd = ProcessPayloadBuilder.update()
                .withProcessInstanceId("1")
                .withBusinessKey("businessKey")
                .withName("name")
                .build();

        mockMvc.perform(put("/v1/process-instances/{processInstanceId}",
                                 1)
                                     .contentType(APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());
    }

    @Test
    public void subprocesses() throws Exception {

        //Simply check here that controller is working
        List<ProcessInstance> processInstanceList = singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstances = new PageImpl<>(processInstanceList,
                                                                processInstanceList.size());

        when(processRuntime.processInstances(any(),any())).thenReturn(processInstances);


        mockMvc.perform(get("/v1/process-instances/{processInstanceId}/subprocesses",
                                 1))
                .andExpect(status().isOk());
    }

    @Test
    public void receiveMessage() throws Exception {
        ReceiveMessagePayload cmd = MessagePayloadBuilder.receive("messageName")
                                                         .withCorrelationKey("correlationId")
                                                         .withVariable("name", "value")
                                                         .build();

        mockMvc.perform(put("/v1/process-instances/message")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd)))
                    .andExpect(status().isOk());
    }

    @Test
    public void startMessage() throws Exception {
        StartMessagePayload cmd = MessagePayloadBuilder.start("messageName")
                                                       .withBusinessKey("buisinessId")
                                                       .withVariable("name", "value")
                                                       .build();

        when(processRuntime.start(any(StartMessagePayload.class))).thenReturn(defaultProcessInstance());

        mockMvc.perform(post("/v1/process-instances/message")
                    .contentType(APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd)))
                    .andExpect(status().isOk());
    }

}
