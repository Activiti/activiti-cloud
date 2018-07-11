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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.runtime.api.cmd.RemoveProcessVariables;
import org.activiti.runtime.api.cmd.impl.RemoveProcessVariablesImpl;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.VariableInstance;
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
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProcessVariablesIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String SIMPLE_PROCESS_WITH_VARIABLES = "ProcessWithVariables";

    @Before
    public void setUp() {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldRetrieveProcessVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Pedro");
        variables.put("lastName",
                "Silva");
        variables.put("age",
                15);
        ResponseEntity<ProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS_WITH_VARIABLES),
                                                                                                 variables);


        await().untilAsserted(() -> {

            //when
            ResponseEntity<Resources<VariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startResponse);
            Collection<VariableInstance> variableCollection = variablesEntity.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName","Pedro",variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName","Silva",variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",15,variableCollection)).isTrue();

        });


    }

    private boolean variablesContainEntry(String key, Object value, Collection<VariableInstance> variableCollection){
        Iterator<VariableInstance> iterator = variableCollection.iterator();
        while(iterator.hasNext()){
            VariableInstance variable = iterator.next();
            if(variable.getName().equalsIgnoreCase(key) && variable.getValue().equals(value)){
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
        ResponseEntity<ProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS_WITH_VARIABLES),
                variables);

        List<String> variablesNames = new ArrayList<>(variables.keySet());

        HttpEntity<RemoveProcessVariables> entity = new HttpEntity<>(new RemoveProcessVariablesImpl(startResponse.getBody().getId(), variablesNames));

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
        ResponseEntity<ProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS_WITH_VARIABLES),
                variables);

        variables.put("firstName",
                "Kermit");
        variables.put("lastName",
                "Frog");
        variables.put("age",
                100);

        //when
        processInstanceRestTemplate.setVariables(startResponse.getBody().getId(), variables);


        await().untilAsserted(() -> {


            // when
        ResponseEntity<Resources<VariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

        // then
        Collection<VariableInstance> variableCollection = variablesResponse.getBody().getContent();

        assertThat(variableCollection).isNotEmpty();
        assertThat(variablesContainEntry("firstName","Kermit",variableCollection)).isTrue();
        assertThat(variablesContainEntry("lastName","Frog",variableCollection)).isTrue();
        assertThat(variablesContainEntry("age",100,variableCollection)).isTrue();

        });

    }

    private ResponseEntity<PagedResources<ProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<ProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<ProcessDefinition>>() {
        };
        return restTemplate.exchange(ProcessDefinitionIT.PROCESS_DEFINITIONS_URL,
                HttpMethod.GET,
                null,
                responseType);
    }
}