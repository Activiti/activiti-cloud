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

package org.activiti.cloud.starter.tests.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProcessVariablesIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String PROCESS_WITH_VARIABLES2 = "ProcessWithVariables2";

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldRetrieveProcessVariablesWithPermission() throws IOException {

        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "Pedro");
        variables.put("lastName",
                      "Silva");
        variables.put("age",
                      15);
        variables.put("boolvar",
                true);
        variables.put("customPojo",objectMapper.readTree("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}")
        );
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                                                                                                      variables);

        await().untilAsserted(() -> {

            //when
            ResponseEntity<Resources<CloudVariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startResponse);
            Collection<CloudVariableInstance> variableCollection = variablesEntity.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                                             "Pedro",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                                             "Silva",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                                             15,
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("boolVar",
                    true,
                    variableCollection)).isTrue();

            assertThat(variableCollection)
                    .filteredOn("name","customPojo")
                    .hasSize(1)
                    .extracting("value")
                    .hasOnlyElementsOfType(LinkedHashMap.class)
                    .first()
                    .toString()
                    .equalsIgnoreCase("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}");

        });
    }

    private boolean variablesContainEntry(String key,
                                          Object value,
                                          Collection<CloudVariableInstance> variableCollection) {
        Iterator<CloudVariableInstance> iterator = variableCollection.iterator();
        while (iterator.hasNext()) {
            VariableInstance variable = iterator.next();
            if (variable.getName().equalsIgnoreCase(key) && variable.getValue().equals(value)) {
                assertThat(variable.getType()).isEqualToIgnoringCase(variable.getValue().getClass().getSimpleName());
                return true;
            }
        }
        return false;
    }

    @Test
    public void shouldDeleteProcessVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "Peter");
        variables.put("lastName",
                      "Silver");
        variables.put("age",
                      19);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                                                                                                      variables);

        List<String> variablesNames = new ArrayList<>(variables.keySet());

        HttpEntity<RemoveProcessVariablesPayload> entity = new HttpEntity<>(ProcessPayloadBuilder
                                                                                    .removeVariables().withProcessInstanceId(startResponse.getBody().getId()).withVariableNames(variablesNames).build());

        //when
        ResponseEntity<Resource<Map<String, Object>>> variablesResponse = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + startResponse.getBody().getId() + "/variables",
                                                                                                HttpMethod.DELETE,
                                                                                                entity,
                                                                                                new ParameterizedTypeReference<Resource<Map<String, Object>>>() {
                                                                                                });

        //then
        assertThat(variablesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldUpdateProcessVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "Peter");
        variables.put("lastName",
                      "Silver");
        variables.put("age",
                      19);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                                                                                                      variables);

        variables.put("firstName",
                      "Kermit");
        variables.put("lastName",
                      "Frog");
        variables.put("age",
                      100);

        //when
        processInstanceRestTemplate.setVariables(startResponse.getBody().getId(),
                                                 variables);

        await().untilAsserted(() -> {

            // when
            ResponseEntity<Resources<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

            // then
            Collection<CloudVariableInstance> variableCollection = variablesResponse.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                                             "Kermit",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                                             "Frog",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                                             100,
                                             variableCollection)).isTrue();
        });
    }

    @Test
    public void shouldNotRetrieveProcessVariablesWithoutPermission() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "Fozzy");
        variables.put("lastName",
                      "Bear");
        variables.put("age",
                      22);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                                                                                                      variables);

        //testuser doesn't have permission according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        await().untilAsserted(() -> {

            ResponseEntity<String> variablesEntity = restTemplate.exchange(ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL + startResponse.getBody().getId() + "/variables",
                                                                           HttpMethod.GET,
                                                                           null,
                                                                           new ParameterizedTypeReference<String>() {
                                                                           });
            assertThat(variablesEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        });
    }

    @Test
    public void adminShouldSeeVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "Rowlf");
        variables.put("lastName",
                      "Dog");
        variables.put("age",
                      5);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                                                                                                      variables);

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        //should see at /{processInstanceId}/variables
        await().untilAsserted(() -> {

            // when
            ResponseEntity<Resources<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

            // then
            Collection<CloudVariableInstance> variableCollection = variablesResponse.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                                             "Rowlf",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                                             "Dog",
                                             variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                                             5,
                                             variableCollection)).isTrue();
        });
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };
        return restTemplate.exchange(ProcessDefinitionIT.PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }
}