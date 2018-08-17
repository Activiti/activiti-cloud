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

import java.util.List;
import java.util.Map;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class ProcessInstanceRestTemplate {

    public static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";

    public static final String PROCESS_INSTANCES_ADMIN_RELATIVE_URL = "/admin/v1/process-instances/";

    @Autowired
    private TestRestTemplate testRestTemplate;

    private ResponseEntity<CloudProcessInstance> startProcess(String processDefinitionKey,
                                                              String processDefinitionId,
                                                              Map<String, Object> variables,
                                                              String businessKey) {

        StartProcessPayload startProcess = ProcessPayloadBuilder.start()
                .withProcessDefinitionId(processDefinitionId)
                .withVariables(variables)
                .withProcessDefinitionKey(processDefinitionKey)
                .withBusinessKey(businessKey)
                .build();

        HttpEntity<StartProcessPayload> requestEntity = new HttpEntity<>(startProcess);

        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL,
                                                                                        HttpMethod.POST,
                                                                                        requestEntity,
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }

    public ResponseEntity<CloudProcessInstance> startProcess(String processDefinitionId) {

        return startProcess(processDefinitionId,
                            null,
                            null);
    }

    public ResponseEntity<CloudProcessInstance> startProcess(String processDefinitionId,
                                                             Map<String, Object> variables) {

        return startProcess(processDefinitionId,
                            variables,
                            null);
    }

    public ResponseEntity<CloudProcessInstance> startProcess(String processDefinitionId,
                                                             Map<String, Object> variables,
                                                             String businessKey) {

        return startProcess(null,
                            processDefinitionId,
                            variables,
                            businessKey);
    }

    public ResponseEntity<CloudProcessInstance> startProcessByKey(String processDefinitionKey,
                                                                  Map<String, Object> variables,
                                                                  String businessKey) {
        return startProcess(processDefinitionKey,
                            null,
                            variables,
                            businessKey);
    }

    public ResponseEntity<PagedResources<CloudTask>> getTasks(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        return getTasks(processInstanceEntity.getBody().getId());
    }

    public ResponseEntity<PagedResources<CloudTask>> getTasks(String processInstanceId) {
        ResponseEntity<PagedResources<CloudTask>> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/tasks",
                                                                                             HttpMethod.GET,
                                                                                             null,
                                                                                             new ParameterizedTypeReference<PagedResources<CloudTask>>() {
                                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Resources<CloudVariableInstance>> getVariables(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        return getVariables(processInstanceEntity.getBody().getId());
    }

    public ResponseEntity<Resources<CloudVariableInstance>> getVariables(String processInstanceId) {
        ResponseEntity<Resources<CloudVariableInstance>> responseEntity = testRestTemplate.exchange(ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/variables",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<Resources<CloudVariableInstance>>() {
                                                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CloudProcessInstance> getProcessInstance(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId(),
                                                                                        HttpMethod.GET,
                                                                                        null,
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> delete(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId(),
                                                                        HttpMethod.DELETE,
                                                                        null,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> suspend(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId() + "/suspend",
                                                                        HttpMethod.POST,
                                                                        null,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> resume(ResponseEntity<CloudProcessInstance> startProcessEntity) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + startProcessEntity.getBody().getId() + "/activate",
                                                                        HttpMethod.POST,
                                                                        null,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> setVariables(String processInstanceId,
                                             Map<String, Object> variables) {
        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder.setVariables()
                .withProcessInstanceId(processInstanceId).withVariables(variables).build();

        HttpEntity<SetProcessVariablesPayload> requestEntity = new HttpEntity<>(
                setProcessVariablesPayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/variables/",
                                                                        HttpMethod.POST,
                                                                        requestEntity,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> removeVariables(String processId,
                                                List<String> variableNames) {
        RemoveProcessVariablesPayload removeProcessVariablesPayload = ProcessPayloadBuilder.removeVariables()
                .withProcessInstanceId(processId).withVariableNames(variableNames).build();

        HttpEntity<RemoveProcessVariablesPayload> requestEntity = new HttpEntity<>(
                removeProcessVariablesPayload,
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