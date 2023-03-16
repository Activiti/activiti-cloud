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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.activiti.cloud.starter.tests.runtime.RuntimeITConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties",
    "classpath:access-control.properties"})
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {KeycloakContainerApplicationInitializer.class})
@Import(TestChannelBinderConfiguration.class)
public class NeverFailDeploymentStrategyIT {

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    @DynamicPropertySource
    public static void signalProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activiti.process-definition-location-prefix", () -> "classpath*:/invalid-processes/");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:test-never-fail");
    }

    @Test
    public void rb_should_startEven_when_itFailsToParseSomeProcessDefinition() {
        //when
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = processDefinitionRestTemplate.getProcessDefinitions();

        //then
        //spring.activiti.process-definition-location-prefix points to a folder containing only an invalid process definition
        //so no process definitions are expected
        assertThat(processDefinitions.getBody()).isNotNull();
        assertThat(processDefinitions.getBody().getContent()).isEmpty();
    }
}
