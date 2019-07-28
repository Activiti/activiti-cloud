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
package org.activiti.cloud.services.notifications.graphql.subscriptions.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.notifications.graphql.subscriptions.GraphQLSubscriptionSchemaBuilder;
import org.activiti.cloud.services.notifications.graphql.subscriptions.GraphQLSubscriptionSchemaProperties;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsPublisherDataFetcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.NONE)
public class GraphQLSubscriptionsAutoConfigurationTest {

    @Autowired
    private GraphQLSubscriptionSchemaProperties properties;

    @Autowired
    private GraphQLSubscriptionSchemaBuilder graphQLSubscriptionSchemaBuilder;

    @Autowired
    private EngineEventsPublisherDataFetcher stompRelayDataFetcher;
    
    @EnableAutoConfiguration
    @Configuration
    static class GraphQLSubscriptionSchemaTestApplication {

    }

    @Test
    public void testGraphQLSubscriptionAutoConfiguration() {
        assertThat(graphQLSubscriptionSchemaBuilder).isNotNull();
    }

    @Test
    public void testGraphQLSubscriptionSchemaProperties() {
        assertThat(properties.getGraphqls()).isEqualTo("classpath:activiti.graphqls");
        assertThat(properties.getSubscriptionFieldName()).isEqualTo("engineEvents");
        assertThat(properties.getSubscriptionArgumentNames()).isEqualTo(new String[]{
                                                                                "serviceName", 
                                                                                "appName", 
                                                                                "eventType",
                                                                                "processDefinitionKey",
                                                                                "processInstanceId",
                                                                                "businessKey"
                                                                            });
    }

}
