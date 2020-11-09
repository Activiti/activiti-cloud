/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplicationAdminController.class)
@Import({
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
public class ApplicationAdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private TaskRepository taskRepository;

    @Test
    public void shouldReturnDeployedApplicationsWhenMediaTypeIsApplicationHalJson() throws Exception {
        //given
        PageRequest pageRequest = PageRequest.of(0,
                10);
        given(applicationRepository.findAll(any(),
                eq(pageRequest)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultApplicationEntity()),
                        pageRequest,
                        1));

        //when
        mockMvc.perform(get("/admin/v1/applications?page=0&size=10")
                .accept(MediaTypes.HAL_JSON_VALUE))
                //then
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnDeployedApplicationsWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        given(applicationRepository.findAll(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultApplicationEntity()),
                        PageRequest.of(1,10),
                        11));

        //when
        mockMvc.perform(get("/admin/v1/applications?skipCount=10&maxItems=10")
                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk());
    }

    private ApplicationEntity buildDefaultApplicationEntity() {
        return new ApplicationEntity(
                UUID.randomUUID().toString(),
                "name",
                "1");
    }
}
