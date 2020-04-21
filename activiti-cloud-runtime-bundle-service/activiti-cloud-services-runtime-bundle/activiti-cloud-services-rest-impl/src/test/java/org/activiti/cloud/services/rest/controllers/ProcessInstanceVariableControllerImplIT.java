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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.DateFormatterProvider;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.variable.VariableValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessInstanceVariableControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        VariableValidationService.class,
        ServicesRestWebMvcAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
public class ProcessInstanceVariableControllerImplIT {

    private static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessRuntime processRuntime;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private TaskAdminRuntime taskAdminRuntime;

    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private MessageChannel commandResults;

    @MockBean
    private DateFormatterProvider dateFormatterProvider;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private CollectionModelAssembler resourcesAssembler;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @BeforeEach
    public void setUp() {
        //this assertion is not really necessary. It's only here to remove warning
        //telling that resourcesAssembler is never used. Even if we are not directly
        //using it in the test we need to to declare it as @SpyBean so it get inject
        //in the controller
        assertThat(resourcesAssembler).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getVariables() throws Exception {
        VariableInstanceImpl<String> name = new VariableInstanceImpl<>("name",
                                                                       String.class.getName(),
                                                                       "Paul",
                                                                       PROCESS_INSTANCE_ID);
        VariableInstanceImpl<Integer> age = new VariableInstanceImpl<>("age",
                                                                       Integer.class.getName(),
                                                                       12,
                                                                       PROCESS_INSTANCE_ID);
        given(processRuntime.variables(any()))
                .willReturn(Arrays.asList(name,
                                          age));

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/variables",
                                 1,
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    public void setVariables() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                      "varObj1");
        variables.put("var2",
                      "varObj2");
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");

        given(processRuntime.processInstance(any()))
        .willReturn(processInstance);

        this.mockMvc.perform(post("/v1/process-instances/{processInstanceId}/variables",
                                  1).contentType(MediaType.APPLICATION_JSON).content(
                mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").withVariables(variables).build())))
                .andExpect(status().isOk());

        verify(processRuntime).setVariables(any());
    }

}
