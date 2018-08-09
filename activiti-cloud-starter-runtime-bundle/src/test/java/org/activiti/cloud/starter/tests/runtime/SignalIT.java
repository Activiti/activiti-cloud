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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.runtime.api.model.CloudProcessDefinition;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.activiti.runtime.api.model.CloudTask;
import org.activiti.runtime.api.model.CloudVariableInstance;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.model.builders.ProcessPayloadBuilder;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SignalIT {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    private static final String SIGNAL_PROCESS = "ProcessWithBoundarySignal";

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Before
    public void setUp() {
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldBroadcastSignals() {
        //when
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        org.activiti.engine.runtime.ProcessInstance procInst2 = ((ProcessEngineConfigurationImpl) processEngineConfiguration).getCommandExecutor().execute(new Command<org.activiti.engine.runtime.ProcessInstance>() {
            public org.activiti.engine.runtime.ProcessInstance execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");
                return runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
            }
        });
        assertThat(procInst1).isNotNull();
        assertThat(procInst2).isNotNull();

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
        org.activiti.engine.runtime.ProcessInstance procInst2 = ((ProcessEngineConfigurationImpl)processEngineConfiguration).getCommandExecutor().execute(new Command<org.activiti.engine.runtime.ProcessInstance>() {
            public org.activiti.engine.runtime.ProcessInstance execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess", Collections.singletonMap("myVar", "myContent"));
                return runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess2");
            }
        });
        assertThat(procInst1).isNotNull();
        assertThat(procInst2).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(procInst1.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask1");
            task = taskService.createTaskQuery().processInstanceId(procInst2.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask1");
        });

        //then
        assertThat(runtimeService.getVariables(procInst1.getId()).get("myVar")).isEqualTo("myContent");
        assertThat(runtimeService.getVariables(procInst2.getId()).get("myVar")).isEqualTo("myContent");
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
        ResponseEntity<PagedResources<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
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

        ResponseEntity<PagedResources<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");

        await().untilAsserted(() -> {
            ResponseEntity<Resources<CloudVariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startProcessEntity);
            Collection<CloudVariableInstance> variableCollection = variablesEntity.getBody().getContent();
            VariableInstance variable = variableCollection.iterator().next();
            assertThat(variable.getName()).isEqualToIgnoringCase("myVar");
            assertThat(variable.<Object>getValue()).isEqualTo("myContent");
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