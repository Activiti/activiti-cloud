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
import org.activiti.cloud.services.api.model.Task.TaskStatus;
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

import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class TaskAdminControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "task-admin";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessEngineWrapper processEngine;

    @Test
    public void getTasks() throws Exception {

        List<Task> taskList = Collections.singletonList(buildDefaultTask());
        Page<Task> tasks = new PageImpl<>(taskList,
                                          PageRequest.of(0,
                                                         10),
                                          taskList.size());
        when(processEngine.getAllTasks(any())).thenReturn(tasks);

        this.mockMvc.perform(get("/admin/v1/tasks"))
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
        when(processEngine.getAllTasks(any())).thenReturn(taskPage);

        this.mockMvc.perform(get("/admin/v1/tasks?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }

    private Task buildDefaultTask() {
        return buildTask(ASSIGNED, "user");
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

}
