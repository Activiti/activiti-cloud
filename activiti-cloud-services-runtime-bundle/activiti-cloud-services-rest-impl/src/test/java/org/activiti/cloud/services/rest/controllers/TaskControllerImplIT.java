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

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.api.task.model.Task.TaskStatus.CREATED;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildDefaultAssignedTask;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildStandAloneTask;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildSubTask;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TaskControllerImpl.class, secure = false)

@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class TaskControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "task";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private TaskRuntime taskRuntime;

    @SpyBean
    private SpringPageConverter springPageConverter;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @Mock
    private Page<Task> taskPage;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        assertThat(springPageConverter).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getTasks() throws Exception {

        List<Task> taskList = Collections.singletonList(buildDefaultAssignedTask());
        Page<Task> tasks = new PageImpl<>(taskList,
                                                                                                             taskList.size());
        when(taskRuntime.tasks(any())).thenReturn(tasks);

        this.mockMvc.perform(get("/v1/tasks"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions."))));
    }

    @Test
    public void getTasksShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        List<Task> taskList = Collections.singletonList(buildDefaultAssignedTask());
        Page<Task> taskPage = new PageImpl<>(taskList,
                                                                                                                taskList.size());
        when(taskRuntime.tasks(any())).thenReturn(taskPage);

        this.mockMvc.perform(get("/v1/tasks?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }

    @Test
    public void getTaskById() throws Exception {
        when(taskRuntime.task("1")).thenReturn(buildDefaultAssignedTask());

        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void claimTask() throws Exception {
        when(securityManager.getAuthenticatedUserId()).thenReturn("assignee");
        given(taskRuntime.claim(any())).willReturn(buildDefaultAssignedTask());

        this.mockMvc.perform(post("/v1/tasks/{taskId}/claim",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/claim",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void releaseTask() throws Exception {

        given(taskRuntime.release(any())).willReturn(buildTask("my task",
                                                                                CREATED));

        this.mockMvc.perform(post("/v1/tasks/{taskId}/release",
                                  1))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/release",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void completeTask() throws Exception {
        given(taskRuntime.complete(any())).willReturn(buildDefaultAssignedTask());
        this.mockMvc.perform(post("/v1/tasks/{taskId}/complete",
                                  1))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/complete",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void deleteTask() throws Exception {
        given(taskRuntime.delete(any())).willReturn(buildDefaultAssignedTask());
        this.mockMvc.perform(delete("/v1/tasks/{taskId}",
                                    1))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/delete",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void getTaskByIdTaskNotFound() throws Exception {
        when(taskRuntime.task("not-existent-task")).thenThrow(new NotFoundException("Not found"));

        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 "not-existent-task"))
                .andExpect(status().isNotFound())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("taskId").description("The task id"))));
    }

    @Test
    public void createNewStandaloneTask() throws Exception {
        TaskImpl task = buildStandAloneTask("new-task",
                                            "New task to be performed");
        given(taskRuntime.create(any())).willReturn(task);

        CreateTaskPayload createTask = TaskPayloadBuilder.create().withName("new-task").withDescription("description").build();
        createTask.setPriority(50);
        this.mockMvc.perform(post("/v1/tasks/",
                                  1).contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(createTask)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/new",
                                links(linkWithRel("self").ignored(),
                                      linkWithRel("claim").description("Link to the claim task resource"),
                                      linkWithRel("home").description("Link to the home resource")
                                ),
                                responseFields(
                                        subsectionWithPath("id").description("The task id."),
                                        subsectionWithPath("owner").description("The task owner."),
                                        subsectionWithPath("createdDate").description("The date where the task task was created."),
                                        subsectionWithPath("claimedDate").description("The date where task was claimed."),
                                        subsectionWithPath("dueDate").description("The date where task is due."),
                                        subsectionWithPath("name").description("The task name."),
                                        subsectionWithPath("description").description("Task description."),
                                        subsectionWithPath("priority").description("Task priority. Can have values between 0 and 100."),
                                        subsectionWithPath("status").description("Task status (can be " + Arrays.asList(Task.TaskStatus.values()) + ")"),
                                        subsectionWithPath("appName").description("The application name"),
                                        subsectionWithPath("appVersion").description("The application version"),
                                        subsectionWithPath("serviceName").description("The service name"),
                                        subsectionWithPath("serviceFullName").description("The full service name"),
                                        subsectionWithPath("serviceType").description("The service type"),
                                        subsectionWithPath("serviceVersion").description("The service version"),
                                        subsectionWithPath("_links").ignored()
                                )));
    }

    @Test
    public void createNewSubtask() throws Exception {
        String parentTaskId = UUID.randomUUID().toString();
        Task subTask = buildSubTask("new-subtask",
                                    "subtask description",
                                    parentTaskId);
        given(taskRuntime.create( any())).willReturn(subTask);

        CreateTaskPayload createTaskCmd = TaskPayloadBuilder.create().withName("new-task").withDescription(
                "description").build();
        createTaskCmd.setPriority(50);
        this.mockMvc.perform(post("/v1/tasks/",
                                  parentTaskId).contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(createTaskCmd)))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/subtasks/get",
                                links(linkWithRel("self").ignored(),
                                      linkWithRel("claim").description("Link to the claim task resource"),
                                      linkWithRel("home").description("Link to the home resource"),
                                      linkWithRel("parent").description("Link to the parent task")
                                ),
                                responseFields(
                                        subsectionWithPath("id").description("The task id."),
                                        subsectionWithPath("owner").description("The task owner."),
                                        subsectionWithPath("createdDate").description("The date where the task task was created."),
                                        subsectionWithPath("claimedDate").description("The date where task was claimed."),
                                        subsectionWithPath("dueDate").description("The date where task is due."),
                                        subsectionWithPath("parentTaskId").description("The parent task id"),
                                        subsectionWithPath("name").description("The task name."),
                                        subsectionWithPath("description").description("Task description."),
                                        subsectionWithPath("priority").description("Task priority. Can have values between 0 and 100."),
                                        subsectionWithPath("status").description("Task status (can be " + Arrays.asList(Task.TaskStatus.values()) + ")"),
                                        subsectionWithPath("appName").description("The application name"),
                                        subsectionWithPath("appVersion").description("The application version"),
                                        subsectionWithPath("serviceName").description("The service name"),
                                        subsectionWithPath("serviceFullName").description("The full service name"),
                                        subsectionWithPath("serviceType").description("The service type"),
                                        subsectionWithPath("serviceVersion").description("The service version"),
                                        subsectionWithPath("_links").ignored()
                                )));
    }

    @Test
    public void getSubtasks() throws Exception {
        final TaskImpl subtask1 = buildTask("subtask-1",
                                            "subtask-1 description");
        subtask1.setPriority(85);

        final TaskImpl subtask2 = buildTask("subtask-2",
                                            "subtask-2 description");
        when(taskPage.getContent()).thenReturn(Arrays.asList(subtask1,
                                                         subtask2));
        when(taskRuntime.tasks(any(), any())).thenReturn(taskPage);

        this.mockMvc.perform(get("/v1/tasks/{taskId}/subtasks",
                                 "parentTaskId").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/subtasks/get",
                                links(halLinks(),
                                      linkWithRel("self").ignored().optional())));
    }
}
