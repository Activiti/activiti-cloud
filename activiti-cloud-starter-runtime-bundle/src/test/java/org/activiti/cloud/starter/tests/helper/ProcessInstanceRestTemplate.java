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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.engine.impl.util.IoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

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

        return startProcess(startProcess);
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
    
    public ResponseEntity<CloudProcessInstance> startProcess(StartProcessPayload startProcess) {
        return startProcess(PROCESS_INSTANCES_RELATIVE_URL,startProcess);
    }
    
    public ResponseEntity<CloudProcessInstance> adminStartProcess(StartProcessPayload startProcess) {
        return startProcess(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,startProcess);
    }
    
    private ResponseEntity<CloudProcessInstance> startProcess(String baseURL,StartProcessPayload startProcess) {
        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(baseURL,
                                                                                        HttpMethod.POST,
                                                                                        new HttpEntity<>(startProcess),
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
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
        assertThat(processInstanceEntity.getBody()).isNotNull();
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
        assertThat(processInstanceEntity.getBody()).isNotNull();
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
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return getProcessInstance(processInstanceEntity.getBody().getId());
    }
    
    public ResponseEntity<CloudProcessInstance> getProcessInstance(String processInstanceId) {
        return getProcessInstance(PROCESS_INSTANCES_RELATIVE_URL,processInstanceId);
    }
    
    public ResponseEntity<CloudProcessInstance> adminGetProcessInstance(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return adminGetProcessInstance(processInstanceEntity.getBody().getId());
    }
    
    public ResponseEntity<CloudProcessInstance> adminGetProcessInstance(String processInstanceId) {
        return getProcessInstance(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,processInstanceId);
    }
    
    public ResponseEntity<CloudProcessInstance> getProcessInstance(String baseURL,String processInstanceId) {

        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(baseURL + processInstanceId,
                                                                                        HttpMethod.GET,
                                                                                        null,
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<CloudProcessInstance> delete(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        assertThat(processInstanceEntity.getBody()).isNotNull();
        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceEntity.getBody().getId(),
                                                                        HttpMethod.DELETE,
                                                                        null,
                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<CloudProcessInstance> adminDelete(ResponseEntity<CloudProcessInstance> processInstanceEntity) {

        assertThat(processInstanceEntity.getBody()).isNotNull();
        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL + processInstanceEntity.getBody().getId(),
                                                                        HttpMethod.DELETE,
                                                                        null,
                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> suspend(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return suspend(PROCESS_INSTANCES_RELATIVE_URL, processInstanceEntity.getBody().getId());
    }
    
    public ResponseEntity<Void> adminSuspend(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return suspend(PROCESS_INSTANCES_ADMIN_RELATIVE_URL, processInstanceEntity.getBody().getId());
    }
    
    private ResponseEntity<Void> suspend(String baseURL, String processInstanceId) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(baseURL + processInstanceId + "/suspend",
                                                                        HttpMethod.POST,
                                                                        null,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        return responseEntity;
    }

    public ResponseEntity<Void> resume(ResponseEntity<CloudProcessInstance> startProcessEntity) {
        assertThat(startProcessEntity.getBody()).isNotNull();
        return resume(PROCESS_INSTANCES_RELATIVE_URL,startProcessEntity.getBody().getId());
    }
    
    public ResponseEntity<Void> adminResume(ResponseEntity<CloudProcessInstance> startProcessEntity) {
        assertThat(startProcessEntity.getBody()).isNotNull();
        return resume(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,startProcessEntity.getBody().getId());
    }
    
    private ResponseEntity<Void> resume(String baseURL, String processInstanceId) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(baseURL + processInstanceId + "/resume",
                                                                        HttpMethod.POST,
                                                                        null,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        return responseEntity;
    }

    public ResponseEntity<Void> setVariables(String processInstanceId,
                                             Map<String, Object> variables) {
        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder.setVariables()
                .withVariables(variables).build();

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
    
    public ResponseEntity<CloudProcessInstance> update(ResponseEntity<CloudProcessInstance> processEntity,
                                                       String businessKey,
                                                       String name
                                                       ) {

        UpdateProcessPayload updateProcessPayload = ProcessPayloadBuilder.update()
                .withProcessInstanceId(processEntity.getBody().getId())
                .withBusinessKey(businessKey)
                .withName(name)
                .build();
        
         return  updateProcess(updateProcessPayload);
    }
    
    private ResponseEntity<CloudProcessInstance> updateProcess(UpdateProcessPayload updateProcessPayload) {
        HttpEntity<UpdateProcessPayload> requestEntity = new HttpEntity<>(updateProcessPayload);

        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL+ updateProcessPayload.getProcessInstanceId(),
                                                                                        HttpMethod.PUT,
                                                                                        requestEntity,
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }
    
    public ResponseEntity<CloudProcessInstance> adminUpdate(ResponseEntity<CloudProcessInstance> processEntity,
                                                       String businessKey,
                                                       String name
                                                       ) {

        UpdateProcessPayload updateProcessPayload = ProcessPayloadBuilder.update()
                .withProcessInstanceId(processEntity.getBody().getId())
                .withBusinessKey(businessKey)
                .withName(name)
                .build();
        
         return  adminUpdateProcess(updateProcessPayload);
    }
    
    private ResponseEntity<CloudProcessInstance> adminUpdateProcess(UpdateProcessPayload updateProcessPayload) {
        HttpEntity<UpdateProcessPayload> requestEntity = new HttpEntity<>(updateProcessPayload);

        ResponseEntity<CloudProcessInstance> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL+ updateProcessPayload.getProcessInstanceId(),
                                                                                        HttpMethod.PUT,
                                                                                        requestEntity,
                                                                                        new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }
    
    public ResponseEntity<PagedResources<ProcessInstance>> getSubprocesses(String processInstanceId) {
        ResponseEntity<PagedResources<ProcessInstance>> responseEntity = testRestTemplate.exchange( 
                                                                                                   PROCESS_INSTANCES_RELATIVE_URL + processInstanceId+"/subprocesses",
                                                                                                   HttpMethod.GET,
                                                                                                   null,
                                                                                                   new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                   });   
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public String getModel(String processInstanceId) {
        return executeRequest(
                              PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/model",
                              HttpMethod.GET,
                              "image/svg+xml");
    }
      
    private String executeRequest(String url,
                                  HttpMethod method,
                                  String contentType) {
        return testRestTemplate.execute(url,
                                        method,
                                        new RequestCallback() {
                                            @Override
                                            public void doWithRequest(ClientHttpRequest request) throws IOException {
                                                if (contentType != null && !contentType.isEmpty()) {
                                                    request.getHeaders().add("Content-Type",
                                                                             contentType);
                                                }
                                            }
                                        },
                                        new ResponseExtractor<String>() {
    
                                            @Override
                                            public String extractData(ClientHttpResponse response)
                                                    throws IOException {
                                                return new String(IoUtil.readInputStream(response.getBody(),
                                                                                         null),
                                                                  "UTF-8");
                                            }
                                        });
    }
    
    public ResponseEntity<PagedResources<ProcessInstance>> getPagedProcessInstances() {
        return getPagedProcessInstances(null);
    }
    
    public ResponseEntity<PagedResources<ProcessInstance>> getPagedProcessInstances(String pages) {
        return getPagedProcessInstances(PROCESS_INSTANCES_RELATIVE_URL,pages);
    }
    
    public ResponseEntity<PagedResources<ProcessInstance>> adminGetPagedProcessInstances() {
        return adminGetPagedProcessInstances(null);
    }
    
    public ResponseEntity<PagedResources<ProcessInstance>> adminGetPagedProcessInstances(String pages) {
        return getPagedProcessInstances(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,pages);
    }
    
    private ResponseEntity<PagedResources<ProcessInstance>> getPagedProcessInstances(String baseURL,String pageFilters) {
        String pages = pageFilters != null ? pageFilters : "page=0&size=2";
        ResponseEntity<PagedResources<ProcessInstance>> responseEntity = testRestTemplate.exchange(
                                                                                        baseURL + "?" + pages,
                                                                                        HttpMethod.GET,
                                                                                        null,
                                                                                        new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
 
}