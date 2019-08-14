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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.activiti.spring.process.model.VariableDefinition;
import org.activiti.spring.process.variable.VariableValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceVariableAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceVariableAdminControllerImplIT {
    @Autowired
    private VariableValidationService variableValidationService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private Map<String, ProcessExtensionModel> processExtensionModelMap;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ResourcesAssembler resourcesAssembler;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        ProcessInstanceImpl processInstance;
        ProcessDefinitionImpl processDefinition;
        ProcessExtensionModel processExtensionModel;

        processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");

        processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("1");
        processDefinition.setKey("1");

        VariableDefinition variableDefinitionName = new VariableDefinition();
        variableDefinitionName.setName("name");
        variableDefinitionName.setType("string");

        VariableDefinition variableDefinitionAge = new VariableDefinition();
        variableDefinitionAge.setName("age");
        variableDefinitionAge.setType("integer");

        VariableDefinition variableDefinitionSubscribe = new VariableDefinition();
        variableDefinitionSubscribe.setName("subscribe");
        variableDefinitionSubscribe.setType("boolean");

        Map<String, VariableDefinition> properties = new HashMap<>();
        properties.put("1", variableDefinitionName);
        properties.put("2", variableDefinitionAge);
        properties.put("3", variableDefinitionSubscribe);

        Extension extension = new Extension();
        extension.setProperties(properties);

        processExtensionModel = new ProcessExtensionModel();
        processExtensionModel.setId("1");
        processExtensionModel.setExtensions(extension);

        given(processAdminRuntime.processInstance(any()))
                .willReturn(processInstance);
        given(processAdminRuntime.processDefinition(any()))
                .willReturn(processDefinition);
        given(processExtensionModelMap.get(any()))
                .willReturn(processExtensionModel);
    }

    @Test
    public void shouldReturn200WithEmptyErrorListWhenSetVariablesWithCorrectNamesAndTypes() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", 24);
        variables.put("subscribe", false);
        String expectedResponseBody = "";

        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isOk());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(expectedResponseBody).isEqualTo(actualResponseBody);
        verify(processAdminRuntime).setVariables(any());
    }

    @Test
    public void shouldReturn400WithErrorListWhenSetVariablesWithWrongNames() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", 24);
        variables.put("subs", false);
        String expectedResponseBody = "[\"Variable with name subs does not exists.\"]";

        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isBadRequest());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(expectedResponseBody).isEqualTo(actualResponseBody);
    }

    @Test
    public void shouldReturn400WithErrorListWhenSetVariablesWithWrongType() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", "24");
        variables.put("subscribe", "false");
        String expectedTypeErrorMessage1 = "class java.lang.String is not assignable from class java.lang.Boolean";
        String expectedTypeErrorMessage2 = "class java.lang.String is not assignable from class java.lang.Integer";

        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isBadRequest());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(actualResponseBody).contains(expectedTypeErrorMessage1);
        assertThat(actualResponseBody).contains(expectedTypeErrorMessage2);
    }

    @Test
    public void shouldReturn400WithErrorListWhenSetVariablesWithWrongNameAndType() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("gender", "female");
        variables.put("age", "24");
        variables.put("subs", true);
        variables.put("subscribe", true);
        String expectedTypeErrorMessage = "class java.lang.String is not assignable from class java.lang.Integer";
        String expectedNameErrorMessage1 = "Variable with name gender does not exists.";
        String expectedNameErrorMessage2 = "Variable with name subs does not exists.";

        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isBadRequest());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(actualResponseBody).contains(expectedTypeErrorMessage);
        assertThat(actualResponseBody).contains(expectedNameErrorMessage1);
        assertThat(actualResponseBody).contains(expectedNameErrorMessage2);
    }

    @Test
    public void deleteVariables() throws Exception {
        this.mockMvc.perform(delete("/admin/v1/process-instances/{processInstanceId}/variables",
                "1")
                .accept(MediaTypes.HAL_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ProcessPayloadBuilder.removeVariables().withVariableNames(Arrays.asList("varName1",
                        "varName2"))
                        .build())))
                .andDo(print())
                .andExpect(status().isOk());
        verify(processAdminRuntime).removeVariables(any());
    }
}