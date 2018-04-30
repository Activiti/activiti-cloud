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

package org.activiti.cloud.starter.tests.helper;

import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.ProcessInstanceVariable;
import org.activiti.cloud.services.api.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class ProcessInstanceRestTemplate {

    public static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";

    @Autowired
    private TestRestTemplate testRestTemplate;


    private ResponseEntity<ProcessInstance> startProcess(String processDefinitionKey,
                                                        String processDefinitionId,
                                                        Map<String, Object> variables,
                                                        String businessKey) {

        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd(processDefinitionKey,
                                                                  processDefinitionId,
                                                                  variables,
                                                                  businessKey);

        HttpEntity<StartProcessInstanceCmd> requestEntity = new HttpEntity<>(cmd);

        ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<ProcessInstance>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }

    public ResponseEntity<ProcessInstance> startProcess(String processDefinitionId) {

        return startProcess(processDefinitionId,
                null,
                null);
    }

    public ResponseEntity<ProcessInstance> startProcess(String processDefinitionId,
                                                        Map<String, Object> variables) {

        return startProcess(processDefinitionId,
                            variables,
                            null);
    }

    public ResponseEntity<ProcessInstance> startProcess(String processDefinitionId,
                                                        Map<String, Object> variables,
                                                        String businessKey) {
        
        return startProcess(null,
                            processDefinitionId,
                            variables,
                            businessKey);
    }

    public ResponseEntity<ProcessInstance> startProcessByKey(String processDefinitionKey,
                                                        Map<String, Object> variables,
                                                        String businessKey) {
        return startProcess(processDefinitionKey,
                            null,
                            variables,
                            businessKey);
    }

    public ResponseEntity<PagedResources<Task>> getTasks(ResponseEntity<ProcessInstance> processInstanceEntity) {


        ResponseEntity<PagedResources<Task>> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId() + "/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PagedResources<Task>>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Resources<ProcessInstanceVariable>> getVariables(ResponseEntity<ProcessInstance> processInstanceEntity) {

        ResponseEntity<Resources<ProcessInstanceVariable>> responseEntity = testRestTemplate.exchange(ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId() + "/variables",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Resources<ProcessInstanceVariable>>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<ProcessInstance> getProcessInstance(ResponseEntity<ProcessInstance> processInstanceEntity) {


        ResponseEntity<ProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ProcessInstance>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> setVariables(String processId, Map<String, Object> variables) {
        SetProcessVariablesCmd processVariablesCmd = new SetProcessVariablesCmd(processId, variables);

        HttpEntity<SetProcessVariablesCmd> requestEntity = new HttpEntity<>(
                processVariablesCmd,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processId + "/variables/",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Void>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> removeVariables(String processId, List<String> variableNames) {
        RemoveProcessVariablesCmd processVariablesCmd = new RemoveProcessVariablesCmd(processId, variableNames);

        HttpEntity<RemoveProcessVariablesCmd> requestEntity = new HttpEntity<>(
                processVariablesCmd,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processId + "/variables/",
                HttpMethod.DELETE,
                requestEntity,
                new ParameterizedTypeReference<Void>() {
                });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
}