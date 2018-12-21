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
package org.activiti.cloud.services.events.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManager;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class CloudEventsAutoConfigurationIT {

    @SpringBootApplication
    static class MockRuntimeBundleApplication {
        @MockBean
        private ProcessEngineChannels processEngineChannels;

        @MockBean
        private RuntimeService runtimeService;

        @MockBean
        private TaskService taskService;

        @MockBean
        private UserGroupManager userGroupManager;

        @MockBean
        private SecurityManager securityManager;

        @MockBean
        private RepositoryService repositoryService;

        @MockBean
        private ProcessSecurityPoliciesManager processSecurityPoliciesManager;
    }
    
    @Value("${spring.cloud.stream.rabbit.bindings.auditProducer.producer.routing-key-expression}")
    private String defaultRoutingKeyExpression;

    @Test
    public void contextLoads() {
        assertThat(defaultRoutingKeyExpression).isEqualTo("headers['routingKey']");
    }

}
