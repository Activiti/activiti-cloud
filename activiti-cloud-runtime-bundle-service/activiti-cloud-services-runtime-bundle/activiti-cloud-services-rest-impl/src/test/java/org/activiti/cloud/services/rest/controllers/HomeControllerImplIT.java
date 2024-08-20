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
package org.activiti.cloud.services.rest.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.services.common.security.config.CommonSecurityAutoConfiguration;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenProvider;
import org.activiti.cloud.services.rest.config.StreamConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeControllerImpl.class)
@AutoConfigureMockMvc
@Import({ StreamConfig.class, CommonSecurityAutoConfiguration.class })
public class HomeControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtAccessTokenProvider jwtAccessTokenProvider;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void getHomeInfo() throws Exception {
        this.mockMvc.perform(get("/v1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Welcome to an instance of the Activiti Process Engine")));
    }
}
