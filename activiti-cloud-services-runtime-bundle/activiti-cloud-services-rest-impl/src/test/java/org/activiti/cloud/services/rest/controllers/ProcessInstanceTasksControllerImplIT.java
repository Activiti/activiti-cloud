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
import java.util.List;

import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.model.Task;
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

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceIdParameter;
import static org.activiti.cloud.services.rest.controllers.TaskSamples.buildDefaultAssignedTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
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
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceTasksControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-instance-tasks";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-instance-tasks-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRuntime taskRuntime;

    @SpyBean
    private SpringPageConverter pageConverter;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        assertThat(pageConverter).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getTasks() throws Exception {
        List<Task> taskList = Collections.singletonList(buildDefaultAssignedTask());
        Page<Task> tasks = new PageImpl<>(taskList,
                                          taskList.size());

        when(taskRuntime.tasks(any(),
                                            any())).thenReturn(tasks);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/tasks",
                                 1,
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pathParameters(parameterWithName("processInstanceId").description("The process instance id")),
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions."))));
    }

    @Test
    public void getTasksShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        Task task = buildDefaultAssignedTask();
        List<Task> taskList = Collections.singletonList(task);
        Page<Task> taskPage = new PageImpl<>(taskList,
                                             taskList.size());

        when(taskRuntime.tasks(any(),
                                            any())).thenReturn(taskPage);

        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}/tasks?skipCount=10&maxItems=10",
                                 task.getProcessInstanceId(),
                                 1).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                processInstanceIdParameter(),
                                pageRequestParameters(),
                                pagedResourcesResponseFields()));
    }
}
