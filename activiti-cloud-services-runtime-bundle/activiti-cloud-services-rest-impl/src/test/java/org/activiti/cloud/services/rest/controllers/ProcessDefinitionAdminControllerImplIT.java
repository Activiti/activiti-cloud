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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.HALDocumentation.pagedProcessDefinitionFields;
import static org.activiti.alfresco.rest.docs.HALDocumentation.selfLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.core.conf.ServicesCoreAutoConfiguration;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestWebMvcAutoConfiguration;
import org.activiti.common.util.conf.ActivitiCoreCommonUtilAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ProcessDefinitionAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ActivitiCoreCommonUtilAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestWebMvcAutoConfiguration.class,
        ServicesCoreAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class})
public class ProcessDefinitionAdminControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-definition";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-definition-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;
    
    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private TaskAdminRuntime taskAdminRuntime;
    
    @MockBean
    private ProcessEngineChannels processEngineChannels;
    
    @MockBean
    private TaskService taskService;

    @MockBean
    private SecurityManager securityManager;
    
    @MockBean
    private MessageChannel commandResults;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        assertThat(processEngineChannels).isNotNull();
        assertThat(processDeployedProducer).isNotNull();
    }

    @Test
    public void getProcessDefinitions() throws Exception {

        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId("procId");
        processDefinition.setName("my process");
        processDefinition.setDescription("this is my process");
        processDefinition.setVersion(1);
        String procId = "procId";
        String my_process = "my process";
        String this_is_my_process = "this is my process";
        int version = 1;
        List<ProcessDefinition> processDefinitionList = Collections.singletonList(buildProcessDefinition(procId,
                my_process,
                this_is_my_process,
                version));
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                processDefinitionList.size());
        when(processAdminRuntime.processDefinitions(any())).thenReturn(processDefinitionPage);

        this.mockMvc.perform(get("/admin/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                pagedProcessDefinitionFields(),
                                links(selfLink())
                ));
    }
    
    private ProcessDefinition buildProcessDefinition(String processDefinitionId,
                                                     String name,
                                                     String description,
                                                     int version) {
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefinitionId);
        processDefinition.setName(name);
        processDefinition.setDescription(description);
        processDefinition.setVersion(version);
        return processDefinition;
    }


    @Test
    public void getProcessDefinitionsShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        String processDefId = UUID.randomUUID().toString();
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processDefId);
        processDefinition.setName("my process");
        processDefinition.setDescription("This is my process");
        processDefinition.setVersion(1);

        List<ProcessDefinition> processDefinitionList = Collections.singletonList(processDefinition);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                11);
        given(processAdminRuntime.processDefinitions(any())).willReturn(processDefinitionPage);

        //when
        MvcResult result = this.mockMvc.perform(get("/admin/v1/process-definitions?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                        pageRequestParameters(),
                        pagedResourcesResponseFields()))
                .andReturn();

        //then
        String responseContent = result.getResponse().getContentAsString();
        assertThatJson(responseContent)
                .node("list.pagination.skipCount").isEqualTo(10)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(11);
        assertThatJson(responseContent)
                .node("list.entries[0].entry.id").isEqualTo(processDefId)
                .node("list.entries[0].entry.name").isEqualTo("my process")
                .node("list.entries[0].entry.description").isEqualTo("This is my process")
                .node("list.entries[0].entry.version").isEqualTo(1);
    }
}
