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
import org.activiti.cloud.services.core.pageable.PageableTaskService;
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
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceTasksControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessInstanceTasksControllerImplTest {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance-tasks";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PageableTaskService pageableTaskService;
    @MockBean
    private TaskResourceAssembler taskResourceAssembler;

    @Test
    public void getTasks() throws Exception {
        List<Task> taskList = new ArrayList<>();
        Page<Task> tasks = new PageImpl<>(taskList,
                                          PageRequest.of(0,
                                                         10),
                                          taskList.size());
        when(pageableTaskService.getTasks(eq("1"),
                                          any())).thenReturn(tasks);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/tasks",
                                 1,
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id")),
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("links").description("The hypermedia links."),
                                               subsectionWithPath("content").description("The process definitions."))));
    }
}
