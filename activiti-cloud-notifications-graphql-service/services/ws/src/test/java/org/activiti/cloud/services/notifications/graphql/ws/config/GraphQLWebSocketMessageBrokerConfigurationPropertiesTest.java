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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.rabbitmq.host=rabbitmq")
public class GraphQLWebSocketMessageBrokerConfigurationPropertiesTest {

    @MockBean
    private GraphQLSchema graphQLSchema;

    @Autowired
    private GraphQLWebSocketMessageBrokerConfigurationProperties configurationProperties;

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfiguration {}

    @Test
    public void testConfigurationProperties() {
        assertThat(configurationProperties.isEnabled()).isEqualTo(true);
        assertThat(configurationProperties.getRelayPort()).isEqualTo(61613);
        assertThat(configurationProperties.getRelayHost()).isEqualTo("rabbitmq"); // overrides from application-graphql-ws.properties
        assertThat(configurationProperties.getClientLogin()).isEqualTo("guest");
        assertThat(configurationProperties.getClientPasscode()).isEqualTo("guest");
        assertThat(configurationProperties.getSystemLogin()).isEqualTo("guest");
        assertThat(configurationProperties.getSystemPasscode()).isEqualTo("guest");
        assertThat(configurationProperties.getAllowedOrigins()).isEqualTo("*");
        assertThat(configurationProperties.getEndpoint()).isEqualTo("/ws/graphql");
        assertThat(configurationProperties.getBufferCount()).isEqualTo(50);
        assertThat(configurationProperties.getBufferTimeSpanMs()).isEqualTo(999); // overrides from graphql-ws.properties
    }
}
