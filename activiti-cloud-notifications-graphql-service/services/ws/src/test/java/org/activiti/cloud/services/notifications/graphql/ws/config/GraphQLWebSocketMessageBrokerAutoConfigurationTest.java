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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import graphql.schema.GraphQLSchema;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerSubProtocolHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class GraphQLWebSocketMessageBrokerAutoConfigurationTest {

    @MockBean
    private GraphQLSchema graphQLSchema;
    
    @Autowired
    private MessageHandler graphQLBrokerMessageHandler;

    @Autowired
    private GraphQLBrokerSubProtocolHandler graphQLBrokerSubProtocolHandler;

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class GraphQLWebSocketMessageBrokerAutoConfigurationTestApplication {

    }

    @Test
    public void testContextLoads() {
        // success
    }

}
