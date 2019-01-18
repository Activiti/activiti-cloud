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
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.api.task.conf.impl.TaskModelAutoConfiguration;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.assemblers.TaskVariableInstanceResourceAssembler;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.taskIdParameter;
import static org.activiti.alfresco.rest.docs.HALDocumentation.unpagedVariableFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TaskVariableControllerImpl.class, secure = false)
@EnableSpringDataWebSupport()
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        TaskModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
public class TaskVariableControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "task-variable";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TaskRuntime taskRuntime;

    @SpyBean
    private TaskVariableInstanceResourceAssembler variableInstanceResourceAssembler;

    @SpyBean
    private ResourcesAssembler resourcesAssembler;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    private static final String TASK_ID = UUID.randomUUID().toString();
    private static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();

    @Before
    public void setUp() {
        //this assertion is not really necessary. It's only here to remove warning
        //telling that resourcesAssembler is never used. Even if we are not directly
        //using it in the test we need to to declare it as @SpyBean so it get inject
        //in the controller
        assertThat(resourcesAssembler).isNotNull();
        assertThat(variableInstanceResourceAssembler).isNotNull();
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getVariables() throws Exception {
        VariableInstanceImpl<String> name = new VariableInstanceImpl<>("name",
                                                                       String.class.getName(),
                                                                       "Paul",
                                                                       PROCESS_INSTANCE_ID);
        name.setTaskId(TASK_ID);
        VariableInstanceImpl<Integer> age = new VariableInstanceImpl<>("age",
                                                                       Integer.class.getName(),
                                                                       12,
                                                                       PROCESS_INSTANCE_ID);
        age.setTaskId(TASK_ID);
        given(taskRuntime.variables(any())).willReturn(Arrays.asList(name,
                                                                                                                          age));
        this.mockMvc.perform(get("/v1/tasks/{taskId}/variables",
                                 TASK_ID).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                taskIdParameter(),
                                unpagedVariableFields()
                       ));
    }


    @Test
    public void setVariables() throws Exception {
        this.mockMvc.perform(post("/v1/tasks/{taskId}/variables/",
                                  TASK_ID).contentType(MediaType.APPLICATION_JSON).content(
                mapper.writeValueAsString(TaskPayloadBuilder.setVariables().withTaskId(TASK_ID)
                                                  .withVariables(Collections.emptyMap()).build())))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/set",
                                pathParameters(parameterWithName("taskId").description("The task id"))));

        verify(taskRuntime).setVariables(any());
    }

}
