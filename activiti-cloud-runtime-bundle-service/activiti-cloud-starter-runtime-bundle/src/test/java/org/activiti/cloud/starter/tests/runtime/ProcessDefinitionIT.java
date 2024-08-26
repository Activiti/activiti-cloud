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
package org.activiti.cloud.starter.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({ "classpath:application-test.properties", "classpath:access-control.properties" })
@ContextConfiguration(
    classes = { RuntimeITConfiguration.class },
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class ProcessDefinitionIT {

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    private final Map<String, String> processDefinitionIds = new HashMap<>();

    @BeforeEach
    public void setUp() {
        identityTokenProducer.withTestUser("hruser");
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = processDefinitionRestTemplate.getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldReturnStartEventStaticMappingsOnlyWhenProcessHasStartEventFormAndMappings() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelStaticValuesMappingForStartEvent(
            processDefinitionIds.get("StartEventStaticMapping")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of("static", "static value"));
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventStaticMappingsAndHasNoStartEventForm() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelStaticValuesMappingForStartEvent(
            processDefinitionIds.get("shouldDeliverMessagesViaRestApi")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventStaticMappingsAndHasNoMappingForStartEvent() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelStaticValuesMappingForStartEvent(
            processDefinitionIds.get("SimpleProcess")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventStaticMappingsAndHasNoExtensions() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelStaticValuesMappingForStartEvent(
            processDefinitionIds.get("ProcessWithVariables")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }

    @Test
    public void shouldReturnStartEventConstantsOnlyWhenProcessHasStartEventFormAndMappings() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelConstantValuesForStartEvent(
            processDefinitionIds.get("StartEventStaticMapping")
        );

        assertThat(staticValues.getBody())
            .isEqualTo(Map.of("startEnabled", "true", "startLabel", "Start the process", "cancelEnabled", "false"));
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventConstantsAndHasNoStartEventForm() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelConstantValuesForStartEvent(
            processDefinitionIds.get("shouldDeliverMessagesViaRestApi")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventConstantsAndHasNoConstantsForStartEvent() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelConstantValuesForStartEvent(
            processDefinitionIds.get("SimpleProcess")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }

    @Test
    public void shouldReturnEmptyMapWhenGettingStartEventConstantsAndHasNoExtensions() {
        ResponseEntity<Map<String, String>> staticValues = processDefinitionRestTemplate.getProcessModelConstantValuesForStartEvent(
            processDefinitionIds.get("ProcessWithVariables")
        );

        assertThat(staticValues.getBody()).isEqualTo(Map.of());
    }
}
