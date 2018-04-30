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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceVariableResourceAssembler;
import org.activiti.engine.RuntimeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
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
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessInstanceVariableControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance-variables";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RuntimeService runtimeService;
    @MockBean
    private ProcessInstanceVariableResourceAssembler variableResourceAssembler;

    @MockBean
    private SecurityPoliciesApplicationService securityService;
    @MockBean
    private ProcessEngineWrapper processEngine;

    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void getVariables() throws Exception {
        when(runtimeService.getVariableInstancesByExecutionIds(any())).thenReturn(new ArrayList<>());

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/variables/",
                                 1,
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void getVariablesLocal() throws Exception {
        when(runtimeService.getVariableInstancesLocal(anyString())).thenReturn(new HashMap<>());

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/variables/local",
                1,
                1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list/local",
                        pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }
    @Test
    public void setVariables() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                "varObj1");
        variables.put("var2",
                "varObj2");

        ProcessInstance processInstance = buildDefaultProcessInstance();
        when(processEngine.getProcessInstanceById(any())).thenReturn(processInstance);
        when(securityService.canWrite(processInstance.getProcessDefinitionKey())).thenReturn(true);

        this.mockMvc.perform(post("/v1/process-instances/{processInstanceId}/variables",
                1).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new SetProcessVariablesCmd("1",
                variables))))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/upsert",
                        pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
    }

    @Test
    public void deleteVariables() throws Exception {
        ProcessInstance processInstance = buildDefaultProcessInstance();
        when(processEngine.getProcessInstanceById(any())).thenReturn(processInstance);
        when(securityService.canRead(processInstance.getProcessDefinitionKey())).thenReturn(true);
        when(securityService.canWrite(processInstance.getProcessDefinitionKey())).thenReturn(true);

        this.mockMvc.perform(delete("/v1/process-instances/{processInstanceId}/variables",
                1)
                .accept(MediaTypes.HAL_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RemoveProcessVariablesCmd("1",Arrays.asList("varName1",
                        "varName2")))))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/delete",
                        pathParameters(parameterWithName("processInstanceId").description("The process instance id"))));
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
}
