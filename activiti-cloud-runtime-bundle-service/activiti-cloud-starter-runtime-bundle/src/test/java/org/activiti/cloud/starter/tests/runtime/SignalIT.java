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

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class SignalIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    private static final String SIGNAL_PROCESS = "ProcessWithBoundarySignal";

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @BeforeEach
    public void setUp() {
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                pd.getId());
        }
    }

    @Test
    public void shouldBroadcastSignals() {
        //when
        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
            assertThat(processInstances).isEmpty();

            processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalEventProcess").list();
            assertThat(processInstances).isEmpty();
        });

    }

    @Test
    public void shouldNotBroadcastSignalsWithProcessInstanceScope() throws InterruptedException {
        //when
        runtimeService.startProcessInstanceByKey("signalThrowEventWithProcessInstanceScopeProcess");

        //then
        long count = runtimeService.createProcessInstanceQuery().processDefinitionKey("signalThrowEventWithProcessInstanceScopeProcess").count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void shouldBroadcastSignalsWithProcessInstanceRest() {
        //when
        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("Test").build();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
            HttpMethod.POST,
            new HttpEntity<>(signalProcessInstancesCmd),
            new ParameterizedTypeReference<Void>() {
            });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
            assertThat(processInstances).isEmpty();
        });

        //then
        List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void shouldBroadcastSignalsWithVariables() {
        //when
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess2");
        runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess", Collections.singletonMap("myVar", "myContent"));

        await("Broadcast Signals").untilAsserted(() -> {
            org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(procInst1.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask1");
        });

        //then
        assertThat(runtimeService.getVariables(procInst1.getId()).get("myVar")).isEqualTo("myContent");
    }

    @Test
    public void shouldBroadcastDifferentSignals() {
        //when
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        org.activiti.engine.runtime.ProcessInstance procInst2 = runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");
        assertThat(procInst1).isNotNull();
        assertThat(procInst2).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst1.getId()).list();
            assertThat(processInstances).isEmpty();
        });

        //then
        List<org.activiti.engine.runtime.ProcessInstance> processInstances1 = runtimeService.createProcessInstanceQuery().processInstanceId(procInst1.getId()).list();
        assertThat(processInstances1).isEmpty();

        org.activiti.engine.runtime.ProcessInstance procInst3 = runtimeService.startProcessInstanceByKey(SIGNAL_PROCESS);
        org.activiti.engine.runtime.ProcessInstance procInst4 = runtimeService.startProcessInstanceByKey("signalThrowEventProcess");
        assertThat(procInst3).isNotNull();
        assertThat(procInst4).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            String taskName = taskService.createTaskQuery().processInstanceId(procInst3.getId()).singleResult().getName();
            assertThat(taskName).isEqualTo("Boundary target");
        });

        //then
        String taskName = taskService.createTaskQuery().processInstanceId(procInst3.getId()).singleResult().getName();
        assertThat(taskName).isEqualTo("Boundary target");
    }

    @Test
    public void processShouldTakeExceptionPathWhenSignalIsSent() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("go").build();

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
            HttpMethod.POST,
            new HttpEntity<>(signalProcessInstancesCmd),
            new ParameterizedTypeReference<Void>() {
            });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<PagedModel<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");
    }

    @Test
    public void processShouldHaveVariablesSetWhenSignalCarriesVariables() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("go").withVariables(
            Collections.singletonMap("myVar",
                "myContent")).build();

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
            HttpMethod.POST,
            new HttpEntity<>(signalProcessInstancesCmd),
            new ParameterizedTypeReference<Void>() {
            });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<PagedModel<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");

        await().untilAsserted(() -> {
            ResponseEntity<CollectionModel<CloudVariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startProcessEntity);
            Collection<CloudVariableInstance> variableCollection = variablesEntity.getBody().getContent();
            VariableInstance variable = variableCollection.iterator().next();
            assertThat(variable.getName()).isEqualToIgnoringCase("myVar");
            assertThat(variable.<Object>getValue()).isEqualTo("myContent");
        });
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
