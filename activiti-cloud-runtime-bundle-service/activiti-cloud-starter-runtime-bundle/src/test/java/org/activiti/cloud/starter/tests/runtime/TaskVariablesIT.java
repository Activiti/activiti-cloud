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

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.cloud.starter.tests.util.VariablesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class TaskVariablesIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private VariablesUtil variablesUtil;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldRetrieveTaskVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
            "test1");
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            variables);
        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startResponse);

        String taskId = tasks.getBody().getContent().iterator().next().getId();

        taskRestTemplate.claim(taskId);

        taskRestTemplate.createVariable(taskId, "var2", "test2");

        //when
        ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = taskRestTemplate.getVariables(taskId);

        //then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2", variablesResponse.getBody().getContent())).isTrue();
        //process variables also at task level
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();

        // give
        taskRestTemplate.updateVariable(taskId, "var2", "test2-update");

        taskRestTemplate.createVariable(taskId, "var3", "test3");

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2-update", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var3", "test3", variablesResponse.getBody().getContent())).isTrue();

        //given
        taskRestTemplate.updateVariable(taskId, "var3", "test3-update");

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2-update", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var3", "test3-update", variablesResponse.getBody().getContent())).isTrue();


    }

    @Test
    public void adminShouldSetGetUpdateTaskVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
            "test1");
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            variables);
        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startResponse);

        String taskId = tasks.getBody().getContent().iterator().next().getId();

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        taskRestTemplate.adminCreateVariable(taskId, "var2", "test2");

        //when
        ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = taskRestTemplate.adminGetVariables(taskId);

        //then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();

        //given
        taskRestTemplate.adminUpdateVariable(taskId, "var2", "test2-update");
        taskRestTemplate.adminCreateVariable(taskId, "var3", "test3");

        // when
        variablesResponse = taskRestTemplate.adminGetVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2-update", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var3", "test3", variablesResponse.getBody().getContent())).isTrue();

        //given
        taskRestTemplate.adminUpdateVariable(taskId, "var3", "test3-update");

        // when
        variablesResponse = taskRestTemplate.adminGetVariables(taskId);

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("var2", "test2-update", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var1", "test1", variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("var3", "test3-update", variablesResponse.getBody().getContent())).isTrue();
    }

    @Test
    public void should_Change_Date_When_CreateUpdateTaskVariables() throws Exception {
        //given
        Date date = new Date();

        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null);
        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startResponse);

        String taskId = tasks.getBody().getContent().iterator().next().getId();

        taskRestTemplate.claim(taskId);

        taskRestTemplate.createVariable(taskId, "variableDateTime", variablesUtil.getDateTimeFormattedString(date));
        taskRestTemplate.createVariable(taskId, "variableDate", variablesUtil.getDateFormattedString(date));

        //when
        ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = taskRestTemplate.getVariables(taskId);

        //then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("variableDateTime", variablesUtil.getExpectedDateTimeFormattedString(date), variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("variableDate", variablesUtil.getExpectedDateFormattedString(date), variablesResponse.getBody().getContent())).isTrue();

        // when
        date = new Date(date.getTime() + 3600000);
        taskRestTemplate.updateVariable(taskId, "variableDateTime", variablesUtil.getDateTimeFormattedString(date));
        taskRestTemplate.updateVariable(taskId, "variableDate", variablesUtil.getDateFormattedString(date));

        // when
        variablesResponse = taskRestTemplate.getVariables(taskId);

        processInstanceRestTemplate.delete(startResponse);
    }

    @Test
    public void admin_Should_Change_Date_When_CreateUpdateTaskVariables() throws Exception {
        //given
        Date date = new Date();

        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS),
            null);
        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startResponse);

        String taskId = tasks.getBody().getContent().iterator().next().getId();

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        taskRestTemplate.adminCreateVariable(taskId, "variableDateTime", variablesUtil.getDateTimeFormattedString(date));
        taskRestTemplate.adminCreateVariable(taskId, "variableDate", variablesUtil.getDateFormattedString(date));

        //when
        ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = taskRestTemplate.adminGetVariables(taskId);

        //then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesContainEntry("variableDateTime", variablesUtil.getExpectedDateTimeFormattedString(date), variablesResponse.getBody().getContent())).isTrue();
        assertThat(variablesContainEntry("variableDate", variablesUtil.getExpectedDateFormattedString(date), variablesResponse.getBody().getContent())).isTrue();

        // when
        date = new Date(date.getTime() + 3600000);
        taskRestTemplate.adminUpdateVariable(taskId, "variableDateTime", variablesUtil.getDateTimeFormattedString(date));
        taskRestTemplate.adminUpdateVariable(taskId, "variableDate", variablesUtil.getDateFormattedString(date));

        // when
        variablesResponse = taskRestTemplate.adminGetVariables(taskId);

        processInstanceRestTemplate.delete(startResponse);
    }

    private boolean variablesContainEntry(String key, Object value, Collection<CloudVariableInstance> variableCollection) {
        Iterator<CloudVariableInstance> iterator = variableCollection.iterator();
        while (iterator.hasNext()) {
            VariableInstance variable = iterator.next();
            if (variable.getName().equalsIgnoreCase(key) && variable.getValue().equals(value)) {
                String type = variable.getType();
                if (type.equalsIgnoreCase("date")) {
                    assertThat("String").isEqualTo(variable.getValue().getClass().getSimpleName());
                } else {
                    assertThat(type).isEqualToIgnoringCase(variable.getValue().getClass().getSimpleName());
                }
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<PagedModel<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {
        };
        return restTemplate.exchange(ProcessDefinitionIT.PROCESS_DEFINITIONS_URL,
            HttpMethod.GET,
            null,
            responseType);
    }
}
