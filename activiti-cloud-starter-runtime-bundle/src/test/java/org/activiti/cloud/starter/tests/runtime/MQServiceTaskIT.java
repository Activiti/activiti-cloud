/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MQServiceTaskIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Value("${activiti.keycloak.test-user:hruser}")
    protected String keycloakTestUser;

    @Before
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser(keycloakTestUser);
    }

    @Test
    public void shouldContinueExecution() {
        //given

        CustomPojo customPojo = new CustomPojo();
        customPojo.setField1("field1");

        CustomPojoAnnotated customPojoAnnotated = new CustomPojoAnnotated();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                      "John");
        variables.put("lastName",
                      "Smith");
        variables.put("age",
                      19);
        variables.put("boolVar",
                      true);
        variables.put("customPojo",
                      customPojo
        );
        variables.put("customPojoAnnotated",
                      customPojoAnnotated);

        //when
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("MQServiceTaskProcess",
                                                                            "businessKey",
                                                                            variables);
        assertThat(procInst).isNotNull();

        //then
        await("the execution should arrive in the human tasks which follows the service task")
                .untilAsserted(() -> {
                                   List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
                                   assertThat(tasks).isNotNull();
                                   assertThat(tasks).extracting(Task::getName).containsExactly("Schedule meeting after service");
                               }
                );

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();

        // the variable "age" should be updated based on ServiceTaskConsumerHandler.receive
        Map<String, Object> updatedVariables = runtimeService.getVariables(procInst.getId());
        assertThat(updatedVariables)
                .containsEntry("firstName",
                               "John")
                .containsEntry("lastName",
                               "Smith")
                .containsEntry("age",
                               20)
                .containsEntry("boolVar",
                               false);

        //engine can resolve annotated pojo in var to correct type but not without annotation
        assertThat(updatedVariables.get("customPojo").getClass()).isEqualTo(ObjectNode.class);
        assertThat(updatedVariables.get("customPojoAnnotated").getClass()).isEqualTo(CustomPojoAnnotated.class);

        assertThat(updatedVariables.get("customPojoTypeInConnector")).isEqualTo("Type of customPojo var in connector is " + LinkedHashMap.class);
        assertThat(updatedVariables.get("customPojoField1InConnector")).isEqualTo("Value of field1 on customPojo is field1");
        assertThat(updatedVariables.get("customPojoAnnotatedTypeInConnector")).isEqualTo("Type of customPojoAnnotated var in connector is " + LinkedHashMap.class);

        //should be able to complete the process
        //when
        taskService.complete(tasks.get(0).getId());

        //then
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst.getId()).list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void integrationContextShouldBeDeletedWhenTheTaskIsCancelled() {
        //given
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("MQServiceTaskWithBoundaryProcess");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
        assertThat(tasks).isEmpty();

        //when boundary is triggered
        runtimeService.signalEventReceived("goPlanB");

        //then the exception path is taken
        tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("Execute plan B");

        //when the task related to the exception path is executed
        taskService.complete(tasks.get(0).getId());

        //the process should finish
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst.getId()).list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void shouldHandleVariableMappings() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
                ProcessPayloadBuilder.start()
                        .withProcessDefinitionKey("connectorVarMapping")
                        .build());

        await().untilAsserted(() -> {
            //when
            ResponseEntity<Resources<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);

            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName,
                                CloudVariableInstance::getValue)
                    .containsOnly(tuple("name",
                                        "outName"),
                                  tuple("age",
                                        25),
                                  tuple("input-unmapped-variable-with-matching-name",
                                        "inTest"),
                                  tuple("input-unmapped-variable-with-non-matching-connector-input-name",
                                        "inTest"),
                                  tuple("nickName",
                                        "testName"),
                                  tuple("out-unmapped-variable-matching-name",
                                        "outTest"),
                                  tuple("output-unmapped-variable-with-non-matching-connector-output-name",
                                        "default"));
        });
    }
}
