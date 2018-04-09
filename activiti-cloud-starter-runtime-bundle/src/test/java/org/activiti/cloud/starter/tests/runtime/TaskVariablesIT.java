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
import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.api.model.TaskVariable;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;

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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskVariablesIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    public static final String TASK_VARIABLES_URL = "/v1/taskId/";

    @Before
    public void setUp() throws Exception {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldRetrieveTaskVariables() throws Exception {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                      "test1");
        ResponseEntity<ProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
                                                                                                 variables);
        ResponseEntity<PagedResources<Task>> tasks = processInstanceRestTemplate.getTasks(startResponse);

        String taskId = tasks.getBody().getContent().iterator().next().getId();
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("var2",
                          "test2");
        taskRestTemplate.setVariablesLocal(taskId, taskVariables);

        //when
        ResponseEntity<Resources<TaskVariable>> variablesResponse = taskRestTemplate.getVariablesLocal(taskId);

        //then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2","test2",variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesDoNotContainKeys(variablesResponse.getBody().getContent(),"var1")).isTrue();

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2","test2",variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1","test1",variablesResponse.getBody().getContent())).isTrue();

        // give
        taskVariables = new HashMap<>();
        taskVariables.put("var2",
                          "test2-update");
        taskVariables.put("var3",
                          "test3");
        taskRestTemplate.setVariables(taskId, taskVariables);

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2","test2-update",variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1","test1",variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var3","test3",variablesResponse.getBody().getContent())).isTrue();

        // when
        variablesResponse = taskRestTemplate.getVariablesLocal(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2","test2-update",variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesDoNotContainKeys(variablesResponse.getBody().getContent(),"var1","var3")).isTrue();

    }

    private boolean variablesContainEntry(String key, Object value, Collection<TaskVariable> variableCollection){
        Iterator<TaskVariable> iterator = variableCollection.iterator();
        while(iterator.hasNext()){
            TaskVariable variable = iterator.next();
            if(variable.getName().equalsIgnoreCase(key) && variable.getValue().equals(value)){
                assertThat(variable.getType()).isEqualToIgnoringCase(variable.getValue().getClass().getSimpleName());
                return true;
            }
        }
        return false;
    }

    private boolean variablesDoNotContainKeys(Collection<TaskVariable> variableCollection, String... keys){
        Iterator<TaskVariable> iterator = variableCollection.iterator();
        while(iterator.hasNext()){
            TaskVariable variable = iterator.next();
            for(String key:keys){
                if(variable.getName().equalsIgnoreCase(key)){
                    return false;
                }
            }
        }
        return true;
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
