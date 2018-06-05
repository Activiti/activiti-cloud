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

import org.activiti.cloud.services.api.commands.SignalCmd;
import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;
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
    public void setUp() throws Exception {
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
        }
    }

    @Test
    public void shouldBroadcastSignals() throws Exception {
        //when
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        org.activiti.engine.runtime.ProcessInstance procInst2 = ((ProcessEngineConfigurationImpl)processEngineConfiguration).getCommandExecutor().execute(new Command<org.activiti.engine.runtime.ProcessInstance>() {
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
    public void shouldBroadcastDifferentSignals() throws Exception {
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
    public void processShouldTakeExceptionPathWhenSignalIsSent() throws Exception {
        //given
        ResponseEntity<ProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalCmd signalProcessInstancesCmd = new SignalCmd("go");

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                HttpMethod.POST,
                new HttpEntity(signalProcessInstancesCmd),
                new ParameterizedTypeReference<Void>() {
                });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<PagedResources<Task>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");
    }

    @Test
    public void processShouldHaveVariablesSetWhenSignalCarriesVariables() throws Exception {
        //given
        ResponseEntity<ProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalCmd signalProcessInstancesCmd = new SignalCmd("go",
                Collections.singletonMap("myVar",
                        "myContent"));

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                HttpMethod.POST,
                new HttpEntity(signalProcessInstancesCmd),
                new ParameterizedTypeReference<Void>() {
                });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<PagedResources<Task>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");

        await().untilAsserted(() -> {
            ResponseEntity<Resources<ProcessInstanceVariable>> variablesEntity = processInstanceRestTemplate.getVariables(startProcessEntity);
            Collection<ProcessInstanceVariable> variableCollection = variablesEntity.getBody().getContent();
            ProcessInstanceVariable variable = variableCollection.iterator().next();
            assertThat(variable.getName()).isEqualToIgnoringCase("myVar");
            assertThat(variable.getValue()).isEqualTo("myContent");
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