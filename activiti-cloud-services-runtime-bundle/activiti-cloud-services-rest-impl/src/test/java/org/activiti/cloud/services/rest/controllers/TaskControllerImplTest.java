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
import java.util.List;

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class TaskControllerImplTest {

    private static final String DOCUMENTATION_IDENTIFIER = "task";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessEngineWrapper processEngine;
    @MockBean
    private TaskResourceAssembler taskResourceAssembler;
    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @Test
    public void getTasks() throws Exception {

        List<Task> taskList = new ArrayList<>();
        Page<Task> tasks = new PageImpl<>(taskList,
                                          PageRequest.of(0,
                                                         10),
                                          taskList.size());
        when(processEngine.getTasks(any())).thenReturn(tasks);

        this.mockMvc.perform(get("/v1/tasks"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("links").description("The hypermedia links."),
                                               subsectionWithPath("content").description("The process definitions."))));
    }

    @Test
    public void getTaskById() throws Exception {
        Task task = mock(Task.class);
        when(processEngine.getTaskById("1")).thenReturn(task);

        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void claimTask() throws Exception {
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("assignee");

        this.mockMvc.perform(post("/v1/tasks/{taskId}/claim",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/claim",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void releaseTask() throws Exception {

        this.mockMvc.perform(post("/v1/tasks/{taskId}/release",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/release",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void completeTask() throws Exception {

        this.mockMvc.perform(post("/v1/tasks/{taskId}/complete",
                                  1))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/complete",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }
}
