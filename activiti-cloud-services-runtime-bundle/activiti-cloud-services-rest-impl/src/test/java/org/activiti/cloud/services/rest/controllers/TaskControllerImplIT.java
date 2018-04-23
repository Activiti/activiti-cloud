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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.api.commands.CreateTaskCmd;
import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.api.model.Task.TaskStatus;
import org.activiti.cloud.services.api.model.converter.ListConverter;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.engine.impl.persistence.entity.TaskEntityImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.cloud.services.api.model.Task.TaskStatus.CREATED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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

    @SpyBean
    private ObjectMapper mapper;
    @SpyBean
    private TaskConverter taskConverter;
    @SpyBean
    private ListConverter listConverter;

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
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }

    private Task buildDefaultTask() {
        return buildTask(ASSIGNED, "user");
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

        given(processEngine.releaseTask(any())).willReturn(buildTask(CREATED, null));

        this.mockMvc.perform(post("/v1/tasks/{taskId}/release",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/release",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    private Task buildTask(TaskStatus status,
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
                        status);
    }

    @Test
    public void completeTask() throws Exception {

        this.mockMvc.perform(post("/v1/tasks/{taskId}/complete",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/complete",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void deleteTask() throws Exception {

        this.mockMvc.perform(delete("/v1/tasks/{taskId}",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/delete",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void getTaskByIdTaskNotFound() throws Exception {
        when(processEngine.getTaskById("not-existent-task")).thenReturn(null);

        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 "not-existent-task"))
                .andExpect(status().isNotFound())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void createNewStandaloneTask() throws Exception {
        final org.activiti.engine.task.Task task = new TaskEntityImpl();
        task.setName("new-task");
        task.setDescription("description");

        final Task taskConverted = taskConverter.from(task);
        when(processEngine.createNewTask(any())).thenReturn(taskConverted);

        this.mockMvc.perform(post("/v1/tasks/",
                                  1).contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(new CreateTaskCmd("new-task",
                                                                                          "description",
                                                                                          null,
                                                                                          50,
                                                                                          null))))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/new",
                                links(linkWithRel("self").ignored(),
                                      linkWithRel("claim").description("Link to the claim task resource"),
                                      linkWithRel("home").description("Link to the home resource")
                                ),
                                responseFields(
                                        subsectionWithPath("name").description("The task name."),
                                        subsectionWithPath("description").description("Task description."),
                                        subsectionWithPath("priority").description("Task priority. Can have values between 0 and 100."),
                                        subsectionWithPath("status").description("Task status (can be " + Arrays.asList(TaskStatus.values()) + ")"),
                                        subsectionWithPath("_links").ignored()
                                )));
    }
}
