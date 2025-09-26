/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.services.connectors.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManager;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CloudConnectorsAutoConfigurationIT {

    @Autowired
    private MQServiceTaskBehavior behavior;

    @MockitoBean
    private IntegrationContextManager integrationContextManager;

    @MockitoBean
    private IntegrationContextService integrationContextService;

    @MockitoBean
    private RuntimeBundleProperties runtimeBundleProperties;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ProcessEngineChannels processEngineChannels;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @MockitoBean
    private ConnectorImplementationsProvider connectorImplementationsProvider;

    @MockitoBean
    private BuildProperties buildProperties;

    @BeforeEach
    public void beforeEach() {
        when(connectorImplementationsProvider.getImplementations()).thenReturn(Collections.emptyList());
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
