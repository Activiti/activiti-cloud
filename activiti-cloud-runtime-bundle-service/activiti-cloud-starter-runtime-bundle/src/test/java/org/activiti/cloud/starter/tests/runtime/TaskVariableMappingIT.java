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
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.cloud.starter.tests.util.VariablesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class TaskVariableMappingIT {

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private VariablesUtil variablesUtil;

    @Value("${activiti.keycloak.test-user:hruser}")
    protected String keycloakTestUser;

    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
    }

    @Test
    public void shouldHandleVariableMappingsForTask() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("taskVarMapping")
                .withBusinessKey("businessKey")
                .build());

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("process_variable_unmapped_1",
                    "unmapped1Value"),
                    tuple("process_variable_inputmap_1",
                        "inputmap1Value"),
                    tuple("process_variable_outputmap_1",
                        "outputmap1Value"));
        });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        assertThat(tasks.getBody()).isNotNull();
        assertThat(tasks.getBody().getContent())
            .extracting(CloudTask::getName)
            .containsExactly("testSimpleTask");

        String taskId = tasks.getBody().getContent().iterator().next().getId();
        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = taskRestTemplate.getVariables(taskId);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("task_input_variable_name_1",
                    "inputmap1Value"));
        });

        Map<String, Object> variables = new HashMap<>();
        variables.put("task_input_variable_name_1", "outputValue");
        variables.put("task_output_variable_name_1", "outputTaskValue");

        claimAndCompleteTask(taskId, variables);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("process_variable_unmapped_1",
                    "unmapped1Value"),
                    tuple("process_variable_inputmap_1",
                        "inputmap1Value"),          //Should be unchanged
                    tuple("process_variable_outputmap_1",
                        "outputTaskValue"));
        });

        //cleanup
        processInstanceRestTemplate.delete(processInstanceResponseEntity);
    }

    @Test
    public void should_Handle_VariableMappingsWithDate() throws Exception {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("taskDateVarMapping")
                .withBusinessKey("businessKey")
                .build());

        //Check default process variables
        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getType)
                .containsOnly(tuple("process_variable_string",
                    "string"),
                    tuple("process_variable_integer",
                        "integer"),
                    tuple("process_variable_boolean",
                        "boolean"),
                    tuple("process_variable_date",
                        "date"),
                    tuple("process_variable_datetime",
                        "date")
                );
        });

        //Check mapped task variables
        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate.getTasks(processInstanceResponseEntity);
        assertThat(tasks.getBody()).isNotNull();

        String taskId = tasks.getBody().getContent().iterator().next().getId();
        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = taskRestTemplate.getVariables(taskId);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getType)
                .containsOnly(tuple("task_variable_string",
                    "string"),
                    tuple("task_variable_integer",
                        "integer"),
                    tuple("task_variable_boolean",
                        "boolean"),
                    tuple("task_variable_date",
                        "date"),
                    tuple("task_variable_datetime",
                        "date")
                );
        });

        //Check mapped process variables
        Date date = new Date();
        Map<String, Object> variables = new HashMap<>();
        variables.put("task_variable_string", "new value");
        variables.put("task_variable_integer", 10);
        variables.put("task_variable_boolean", false);
        variables.put("task_variable_date", variablesUtil.getDateFormattedString(date));
        variables.put("task_variable_datetime", variablesUtil.getDateTimeFormattedString(date));

        claimAndCompleteTask(taskId, variables);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                .isNotNull()
                .extracting(CloudVariableInstance::getName,
                    CloudVariableInstance::getType,
                    CloudVariableInstance::getValue)
                .containsOnly(tuple("process_variable_string",
                    "string",
                    "new value"),
                    tuple("process_variable_integer",
                        "integer",
                        10),
                    tuple("process_variable_boolean",
                        "boolean",
                        false),
                    tuple("process_variable_date",
                        "date",
                        variablesUtil.getExpectedDateFormattedString(date)),
                    tuple("process_variable_datetime",
                        "date",
                        variablesUtil.getExpectedDateTimeFormattedString(date))
                );
        });

        //cleanup
        processInstanceRestTemplate.delete(processInstanceResponseEntity);
    }

    private void claimAndCompleteTask(String taskId, Map<String, Object> variables) {
        //claim task
        ResponseEntity<CloudTask> claimTask = taskRestTemplate.claim(taskId);
        assertThat(claimTask.getBody()).isNotNull();
        assertThat(claimTask.getBody().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        CompleteTaskPayload completeTaskPayload = TaskPayloadBuilder
            .complete()
            .withTaskId(taskId)
            .withVariables(variables)
            .build();

        ResponseEntity<CloudTask> completeTask = taskRestTemplate.complete(claimTask.getBody(), completeTaskPayload);
        assertThat(completeTask.getBody()).isNotNull();
        assertThat(completeTask.getBody().getStatus()).isEqualTo(TaskStatus.COMPLETED);

    }
}
