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

import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.runtime.api.security.SecurityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.variableFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.variableIdParameter;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(VariableController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.query.rest.assembler", "org.activiti.cloud.alfresco"})
public class VariableEntityControllerIT {

    private static final String VARIABLE_ALFRESCO_IDENTIFIER = "variable-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VariableRepository variableRepository;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @Test
    public void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        VariableEntity variableEntity = new VariableEntity(String.class.getName(),
                "firstName",
                UUID.randomUUID().toString(),
                "My app",
                "My app",
                "1",
                null,
                null,
                UUID.randomUUID().toString(),
                new Date(),
                new Date(),
                UUID.randomUUID().toString());
        variableEntity.setValue("John");
        given(entityFinder.findById(eq(variableRepository),
                eq(variableEntity.getId()),
                anyString()))
                .willReturn(variableEntity);

        //when
        this.mockMvc.perform(get("/v1/variables/{variableId}",
                variableEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(VARIABLE_ALFRESCO_IDENTIFIER + "/get",
                        variableIdParameter(),
                        variableFields()
                        )
                );

    }
}