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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.junit.Before;
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
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceIdParameter;
import static org.activiti.alfresco.rest.docs.HALDocumentation.unpagedVariableFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceVariableControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceVariableControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance-variables";
    private static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessRuntime processRuntime;

    @Autowired
    private ObjectMapper mapper;

    @SpyBean
    private ResourcesAssembler resourcesAssembler;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
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
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                processInstanceIdParameter(),
                                unpagedVariableFields()
                       ));
    }

    @Test
    public void setVariables() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                      "varObj1");
        variables.put("var2",
                      "varObj2");

        this.mockMvc.perform(post("/v1/process-instances/{processInstanceId}/variables",
                                  1).contentType(MediaType.APPLICATION_JSON).content(
                mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").withVariables(variables).build())))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/upsert",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));

        verify(processRuntime).setVariables(any());
    }

    @Test
    public void deleteVariables() throws Exception {
        this.mockMvc.perform(delete("/v1/process-instances/{processInstanceId}/variables",
                                    PROCESS_INSTANCE_ID)
                                     .accept(MediaTypes.HAL_JSON_VALUE)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(ProcessPayloadBuilder.removeVariables().withProcessInstanceId(PROCESS_INSTANCE_ID).withVariableNames(Arrays.asList("varName1",
                                                                                                                                                                                           "varName2")).build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/delete",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
        verify(processRuntime).removeVariables(any());
    }
}
