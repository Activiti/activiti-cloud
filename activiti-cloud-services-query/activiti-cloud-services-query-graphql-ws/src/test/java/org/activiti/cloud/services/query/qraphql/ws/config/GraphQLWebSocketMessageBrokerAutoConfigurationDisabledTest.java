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
package org.activiti.cloud.services.query.qraphql.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.query.qraphql.ws.config.GraphQLSubscriptionsSchemaAutoConfiguration.DefaultGraphQLSubscriptionsSchemaConfiguration;
import org.activiti.cloud.services.query.qraphql.ws.config.GraphQLWebSocketMessageBrokerAutoConfiguration.DefaultGraphQLWebSocketMessageBrokerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties="spring.activiti.cloud.services.query.graphql.ws.enabled=false")
public class GraphQLWebSocketMessageBrokerAutoConfigurationDisabledTest {

    @Autowired(required=false)
    private DefaultGraphQLWebSocketMessageBrokerConfiguration defaultGraphQLWebSocketMessageBrokerConfiguration;

    @Autowired(required=false)
    private DefaultGraphQLSubscriptionsSchemaConfiguration defaultGraphQLSubscriptionsSchemaConfiguration;

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class GraphQLWebSocketMessageBrokerAutoConfigurationTestApplication {

    }

    @Test
    public void testContextLoads() throws InterruptedException {
        assertThat(defaultGraphQLWebSocketMessageBrokerConfiguration).isNull();
        assertThat(defaultGraphQLSubscriptionsSchemaConfiguration).isNull();
    }
}
