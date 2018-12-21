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

package org.activiti.services.connectors.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManager;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CloudConnectorsAutoConfigurationIT {

    private static final String HEADERS_ROUTING_KEY = "headers['routingKey']";

    @Autowired
    private MQServiceTaskBehavior behavior;

    @MockBean
    private IntegrationContextManager integrationContextManager;

    @MockBean
    private RuntimeBundleProperties runtimeBundleProperties;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private TaskService taskService;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;
    
    @Value("${activiti.spring.cloud.stream.connector.integrationRequestSender.routing-key-expression}")
    private String routingKeyExpression;
    

    @Before
    public void setUp() {
        doNothing().when(processDeployedProducer).onApplicationEvent(any());
    }

    @Test
    public void shouldProvideRoutingKeyExpression() {
        assertThat(routingKeyExpression).isEqualTo(HEADERS_ROUTING_KEY);
    }
    
    @Test
    public void shouldProvideMQServiceTaskBehaviorBean() {
        assertThat(behavior).isNotNull();
    }

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class CloudConnectorsAutoConfigurationITApplication {

        @Bean
        public RepositoryService repositoryService() {
            return mock(RepositoryService.class);
        }

        @Bean
        public RuntimeService runtimeService() {
            return mock(RuntimeService.class);
        }

        @Bean
        public UserGroupManager userGroupManager() {
            return mock(UserGroupManager.class);
        }

        @Bean
        public SecurityManager securityManager() {
            return mock(SecurityManager.class);
        }

        @Bean
        public ProcessSecurityPoliciesManager processSecurityPoliciesManager() {
            return mock(ProcessSecurityPoliciesManager.class);
        }

    }
}