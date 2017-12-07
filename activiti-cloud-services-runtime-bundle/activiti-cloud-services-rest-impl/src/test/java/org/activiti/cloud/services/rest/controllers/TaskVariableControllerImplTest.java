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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.rest.assemblers.TaskVariablesResourceAssembler;
import org.activiti.engine.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskVariableControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class TaskVariableControllerImplTest {

    private static final String DOCUMENTATION_IDENTIFIER = "task-variable";
    @MockBean
    private TaskVariablesResourceAssembler variableResourceAssembler;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ProcessEngineWrapper processEngine;
    @MockBean
    private TaskService taskService;
    @SpyBean
    private ObjectMapper mapper;

    @Test
    public void getVariables() throws Exception {
        this.mockMvc.perform(get("/v1/tasks/{taskId}/variables/",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void getVariablesLocal() throws Exception {

        this.mockMvc.perform(get("/v1/tasks/{taskId}/variables/local",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list/local",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void setVariables() throws Exception {
        this.mockMvc.perform(post("/v1/tasks/{taskId}/variables/",
                                  1).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new SetTaskVariablesCmd("1",
                                                                                                                                       Collections.emptyMap()))))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/set",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void setVarisetVariablesLocalables() throws Exception {
        this.mockMvc.perform(post("/v1/tasks/{taskId}/variables/local",
                                  1).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new SetTaskVariablesCmd("1",
                                                                                                                                       Collections.emptyMap()))))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/set/local",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }
}
