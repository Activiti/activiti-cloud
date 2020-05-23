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
package org.activiti.cloud.starter.tests.conf;

import org.activiti.cloud.starter.rb.behavior.CloudActivityBehaviorFactory;
import org.activiti.runtime.api.impl.MappingAwareActivityBehaviorFactory;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class EngineConfigurationIT {

    @Autowired
    private SpringProcessEngineConfiguration configuration;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldUseCloudActivityBehaviorFactory() {
        assertThat(configuration.getActivityBehaviorFactory())
            .isInstanceOf(MappingAwareActivityBehaviorFactory.class)
            .isInstanceOf(CloudActivityBehaviorFactory.class);

        assertThat(configuration.getBpmnParser().getActivityBehaviorFactory())
            .isInstanceOf(MappingAwareActivityBehaviorFactory.class)
            .isInstanceOf(CloudActivityBehaviorFactory.class);
    }

    @Test
    public void shouldHaveRequiredGroupsSetForAuditProducer() {
        //when
        String requiredGroups = applicationContext.getEnvironment().getProperty("spring.cloud.stream.bindings.auditProducer.producer.required-groups");
        //then
        assertThat(requiredGroups).isEqualTo("query,audit");
    }
}
