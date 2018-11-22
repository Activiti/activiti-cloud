/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.query.rest;

import java.util.Collections;
import java.util.UUID;

import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.conf.QueryRestAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.alfrescoPagedProcessDefinitions;
import static org.activiti.alfresco.rest.docs.HALDocumentation.pagedProcessDefinitionFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessDefinitionAdminController.class)
@Import({
        QueryRestAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.query.rest.assembler", "org.activiti.cloud.alfresco"})
public class ProcessDefinitionAdminControllerIT {

    private static final String PROCESS_DEFINITION_IDENTIFIER = "process-definition";
    private static final String ALFRESCO_PROCESS_DEFINITION_IDENTIFIER = "process-definition-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockBean
    private SecurityManager securityManager;

    @Before
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    public void shouldReturnAvailableProcessDefinitions() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(0,
                                                 10);
        given(processDefinitionRepository.findAll(any(),
                                                  eq(pageRequest)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultProcessDefinition()),
                                           pageRequest,
                                           1));

        //when
        mockMvc.perform(get("/admin/v1/process-definitions?page=0&size=10")
                                .accept(MediaTypes.HAL_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_DEFINITION_IDENTIFIER + "/list",
                                links(selfLink()),
                                pagedProcessDefinitionFields()));

    }
    private LinkDescriptor selfLink() {
        return linkWithRel("self").description("URL called to get this result");
    }

    private ProcessDefinitionEntity buildDefaultProcessDefinition() {
        ProcessDefinitionEntity processDefinitionEntity = new ProcessDefinitionEntity();
        processDefinitionEntity.setId(UUID.randomUUID().toString());
        processDefinitionEntity.setName("My Process");
        processDefinitionEntity.setKey("myProcess");
        processDefinitionEntity.setVersion(1);
        processDefinitionEntity.setDescription("This is my process description");
        return processDefinitionEntity;
    }

    @Test
    public void shouldReturnAvailableProcessDefinitionsUsingAlfrescoFormat() throws Exception {
        //given
        given(processDefinitionRepository.findAll(any(),
                                                  ArgumentMatchers.<Pageable>any()))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultProcessDefinition()),
                                           PageRequest.of(1,
                                                                                    10),
                                           11));

        //when
        mockMvc.perform(get("/admin/v1/process-definitions?skipCount=10&maxItems=10")
                                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(ALFRESCO_PROCESS_DEFINITION_IDENTIFIER + "/list",
                                alfrescoPagedProcessDefinitions()));

    }

}
