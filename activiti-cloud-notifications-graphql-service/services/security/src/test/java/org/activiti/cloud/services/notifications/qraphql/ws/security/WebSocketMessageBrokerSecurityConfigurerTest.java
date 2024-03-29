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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebSocketMessageBrokerSecurityConfigurerTest {

    @Autowired
    private WebSocketMessageBrokerSecurityConfigurer configuration;

    @MockBean
    private BuildProperties buildProperties;

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class GraphQLSecurityWebSocketMessageBrokerConfigurationTestApplication {}

    @Test
    public void testContextLoads() {
        assertThat(configuration.getEndpoint()).isEqualTo("/ws/graphql");
        assertThat(configuration.getAuthorities()).containsExactly("ACTIVITI_USER", "ACTIVITI_ADMIN");
        assertThat(configuration.sameOriginDisabled()).isTrue();
    }
}
