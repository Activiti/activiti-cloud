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

import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    public void setUp() throws Exception {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldRetrieveProcessVariables() throws Exception {
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
            ResponseEntity<Resources<ProcessInstanceVariable>> variablesEntity = processInstanceRestTemplate.getVariables(startResponse);
            Collection<ProcessInstanceVariable> variableCollection = variablesEntity.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            Iterator<ProcessInstanceVariable> iterator = variableCollection.iterator();
            while(iterator.hasNext()){
                ProcessInstanceVariable variable = iterator.next();
                assertThat(variable.getName()).isIn("firstName","lastName","age");
                assertThat(variable.getProcessInstanceId()).isEqualToIgnoringCase(startResponse.getBody().getId());
                assertThat(variable.getValue()).isIn("Pedro","Silva",15);
                assertThat(variable.getType()).isNotEmpty();
                if(variable.getValue().equals(15)){
                    assertThat(variable.getType()).isEqualToIgnoringCase(Integer.class.getSimpleName());
                } else{
                    assertThat(variable.getType()).isEqualToIgnoringCase(String.class.getSimpleName());
                }
            }
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