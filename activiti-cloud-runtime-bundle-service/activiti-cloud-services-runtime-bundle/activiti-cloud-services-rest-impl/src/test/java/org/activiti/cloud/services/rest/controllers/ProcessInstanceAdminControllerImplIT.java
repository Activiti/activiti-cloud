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

import static org.activiti.cloud.services.rest.controllers.ProcessInstanceSamples.defaultProcessInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessInstanceAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        ServicesCoreAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class ProcessInstanceAdminControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private TaskAdminRuntime taskAdminRuntime;

    @MockBean
    private MessageChannel commandResults;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @BeforeEach
    public void setUp() {
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getProcessInstances() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstances = new PageImpl<>(processInstanceList,
                processInstanceList.size());
        when(processAdminRuntime.processInstances(any())).thenReturn(processInstances);

        this.mockMvc.perform(get("/admin/v1/process-instances?page=0&size=10")
                             .accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    public void getProcessInstancesShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {

        List<ProcessInstance> processInstanceList = Collections.singletonList(defaultProcessInstance());
        Page<ProcessInstance> processInstancePage = new PageImpl<>(processInstanceList,
                processInstanceList.size());
        when(processAdminRuntime.processInstances(any())).thenReturn(processInstancePage);

        this.mockMvc.perform(get("/admin/v1/process-instances?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void resume() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(processAdminRuntime.processInstance("1")).thenReturn(processInstance);

        when(processAdminRuntime.resume(any())).thenReturn(defaultProcessInstance());

        this.mockMvc.perform(post("/admin/v1/process-instances/{processInstanceId}/resume",
                1))
                .andExpect(status().isOk());
    }

    @Test
    public void suspend() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processAdminRuntime.processInstance("1")).thenReturn(processInstance);
        when(processAdminRuntime.suspend(any())).thenReturn(defaultProcessInstance());
        this.mockMvc.perform(post("/admin/v1/process-instances/{processInstanceId}/suspend", 1))
               .andExpect(status().isOk());
    }

    @Test
    public void deleteProcessInstance() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processAdminRuntime.processInstance("1")).thenReturn(processInstance);
        when(processAdminRuntime.delete(any())).thenReturn(defaultProcessInstance());
        this.mockMvc.perform(delete("/admin/v1/process-instances/{processInstanceId}",
                                    1))
                .andExpect(status().isOk());
    }

    @Test
    public void update() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processAdminRuntime.processInstance("1")).thenReturn(processInstance);
        when(processAdminRuntime.update(any())).thenReturn(defaultProcessInstance());

        UpdateProcessPayload cmd = ProcessPayloadBuilder.update()
                .withProcessInstanceId("1")
                .withBusinessKey("businessKey")
                .withName("name")
                .build();

        this.mockMvc.perform(put("/admin/v1/process-instances/{processInstanceId}",
                                 1)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());

    }

    @Test
    public void startMessage() throws Exception {
        StartMessagePayload cmd = MessagePayloadBuilder.start("messageName")
                                                       .withBusinessKey("buisinessId")
                                                       .withVariable("name", "value")
                                                       .build();

        when(processAdminRuntime.start(any(StartMessagePayload.class))).thenReturn(defaultProcessInstance());

        this.mockMvc.perform(post("/admin/v1/process-instances/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd)))
                    .andExpect(status().isOk());
    }

    @Test
    public void receiveMessage() throws Exception {
        ReceiveMessagePayload cmd = MessagePayloadBuilder.receive("messageName")
                                                         .withCorrelationKey("correlationId")
                                                         .withVariable("name", "value")
                                                         .build();

        this.mockMvc.perform(put("/admin/v1/process-instances/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(cmd)))
                    .andExpect(status().isOk());
    }

}
