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

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CloudConnectorsAutoConfigurationIT {
    
    @Autowired
    private MQServiceTaskBehavior behavior;

    @MockBean
    private IntegrationContextManager integrationContextManager;

    @MockBean
    private RuntimeBundleProperties runtimeBundleProperties;

    @MockBean
    private ApplicationEventPublisher eventPublisher;


    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class CloudConnectorsAutoConfigurationITApplication {

    }

    @Test
    public void shouldProvideMQServiceTaskBehaviorBean() {
        assertThat(behavior).isNotNull();
    }
}