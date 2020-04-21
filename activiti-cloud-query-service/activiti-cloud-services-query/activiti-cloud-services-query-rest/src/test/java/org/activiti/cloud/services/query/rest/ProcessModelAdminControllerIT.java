/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@WebMvcTest(ProcessModelAdminController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@Import({
    QueryRestWebMvcAutoConfiguration.class,
    CommonModelAutoConfiguration.class,
    AlfrescoWebAutoConfiguration.class
})
public class ProcessModelAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessModelRepository processModelRepository;

    @MockBean
    private UserGroupManager userGroupManager;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private EntityFinder entityFinder;

    @Test
    public void shouldReturnProcessModelById() throws Exception {
        //given
        String processDefinitionId = UUID.randomUUID().toString();

        given(entityFinder.findById(eq(processModelRepository), eq(processDefinitionId), anyString()))
                .willReturn(new ProcessModelEntity(new ProcessDefinitionEntity(), "<model/>"));

        //when
        mockMvc.perform(get("/admin/v1/process-definitions/{processDefinitionId}/model",
                            processDefinitionId)
                                .accept(MediaType.APPLICATION_XML_VALUE))
                //then
                .andExpect(status().isOk())
                .andExpect(content().xml("<model/>"));
    }

}
