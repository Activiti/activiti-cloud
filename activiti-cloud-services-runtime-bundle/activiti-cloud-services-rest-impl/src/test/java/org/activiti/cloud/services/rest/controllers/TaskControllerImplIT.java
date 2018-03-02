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

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class TaskControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "task";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessEngineWrapper processEngine;
    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @Test
    public void getTasks() throws Exception {

        List<Task> taskList = Collections.singletonList(buildDefaultTask());
        Page<Task> tasks = new PageImpl<>(taskList,
                                          PageRequest.of(0,
                                                         10),
                                          taskList.size());
        when(processEngine.getTasks(any())).thenReturn(tasks);

        this.mockMvc.perform(get("/v1/tasks"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions."))));
    }

    @Test
    public void getTasksShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        List<Task> taskList = Collections.singletonList(buildDefaultTask());
        Page<Task> taskPage = new PageImpl<>(taskList,
                                          PageRequest.of(1,
                                                         10),
                                          taskList.size());
        when(processEngine.getTasks(any())).thenReturn(taskPage);

        this.mockMvc.perform(get("/v1/tasks?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                requestParameters(
                                        parameterWithName("skipCount")
                                                .description("How many entities exist in the entire addressed collection before those included in this list."),
                                        parameterWithName("maxItems")
                                                .description("The max number of entities that can be included in the result.")
                                ),
                                responseFields(
                                        subsectionWithPath("list").ignored(),
                                        subsectionWithPath("list.entries").description("List of results."),
                                        subsectionWithPath("list.entries[].entry").description("Wrapper for each entry in the list of results."),
                                        subsectionWithPath("list.pagination").description("Pagination metadata."),
                                        subsectionWithPath("list.pagination.skipCount")
                                                .description("How many entities exist in the entire addressed collection before those included in this list."),
                                        subsectionWithPath("list.pagination.maxItems")
                                                .description("The maxItems parameter used to generate this list."),
                                        subsectionWithPath("list.pagination.count")
                                                .description("The number of entities included in this list. This number must correspond to the number of objects in the \"entries\" array."),
                                        subsectionWithPath("list.pagination.hasMoreItems")
                                                .description("A boolean value that indicates whether there are further entities in the addressed collection beyond those returned " +
                                                                     "in this response. If true then a request with a larger value for either the skipCount or the maxItems " +
                                                                     "parameter is expected to return further results."),
                                        subsectionWithPath("list.pagination.totalItems")
                                                .description("An integer value that indicates the total number of entities in the addressed collection.")
                                )));
    }

    private Task buildDefaultTask() {
        return buildTask(Task.TaskStatus.ASSIGNED, "user");
    }

    @Test
    public void getTaskById() throws Exception {
        when(processEngine.getTaskById("1")).thenReturn(buildDefaultTask());

        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void claimTask() throws Exception {
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("assignee");
        given(processEngine.claimTask(any())).willReturn(buildDefaultTask());

        this.mockMvc.perform(post("/v1/tasks/{taskId}/claim",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/claim",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void releaseTask() throws Exception {

        given(processEngine.releaseTask(any())).willReturn(buildTask(Task.TaskStatus.CREATED, null));

        this.mockMvc.perform(post("/v1/tasks/{taskId}/release",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/release",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    private Task buildTask(Task.TaskStatus status,
                           String assignee) {
        return new Task(UUID.randomUUID().toString(),
                        "user",
                        assignee,
                        "Validate",
                        "Validate request",
                        new Date(),
                        new Date(),
                        new Date(),
                        10,
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        null,
                        status.name());
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
