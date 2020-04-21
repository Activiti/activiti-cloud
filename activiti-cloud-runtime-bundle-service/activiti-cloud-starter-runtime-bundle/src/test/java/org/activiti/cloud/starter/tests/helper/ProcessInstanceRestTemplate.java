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
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.engine.impl.util.IoUtil;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

@TestComponent
public class ProcessInstanceRestTemplate {

    public static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";

    public static final String PROCESS_INSTANCES_ADMIN_RELATIVE_URL = "/admin/v1/process-instances/";

    private TestRestTemplate testRestTemplate;

    public ProcessInstanceRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

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

    private ResponseEntity<CloudProcessInstance> createProcess(String processDefinitionKey,
                                                              String processDefinitionId,
                                                              Map<String, Object> variables,
                                                              String businessKey) {

        StartProcessPayload startProcess = ProcessPayloadBuilder.start()
            .withProcessDefinitionId(processDefinitionId)
            .withVariables(variables)
            .withProcessDefinitionKey(processDefinitionKey)
            .withBusinessKey(businessKey)
            .build();

        return createProcess(startProcess);
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

    public ResponseEntity<CloudProcessInstance> createProcess(String processDefinitionId,
                                                             Map<String, Object> variables,
                                                             String businessKey) {

        return createProcess(null,
            processDefinitionId,
            variables,
            businessKey);
    }

    public ResponseEntity<CloudProcessInstance> startProcess(StartProcessPayload startProcess) {
        return startProcess(PROCESS_INSTANCES_RELATIVE_URL,startProcess);
    }

    public ResponseEntity<CloudProcessInstance> createProcess(StartProcessPayload startPayload) {
        return createProcess(PROCESS_INSTANCES_RELATIVE_URL + "create", startPayload);
    }

    public ResponseEntity<CloudProcessInstance> adminStartProcess(StartProcessPayload startProcess) {
        return startProcess(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,startProcess);
    }

    private ResponseEntity<CloudProcessInstance> startProcessWithoutCheck(String baseURL, StartProcessPayload startProcess) {
        return  testRestTemplate.exchange(baseURL,
                                          HttpMethod.POST,
                                          new HttpEntity<>(startProcess),
                                          new ParameterizedTypeReference<CloudProcessInstance>() {
                                          });
    }

    private ResponseEntity<CloudProcessInstance> startCreatedProcessCall(String baseURL) {
        return testRestTemplate.exchange(baseURL,
            HttpMethod.POST,
            new HttpEntity<>(null),
            new ParameterizedTypeReference<CloudProcessInstance>() {
            });
    }

    private ResponseEntity<ActivitiErrorMessageImpl> startCreatedProcessCallFail(String baseURL) {
        return testRestTemplate.exchange(baseURL,
            HttpMethod.POST,
            new HttpEntity<>(null),
            new ParameterizedTypeReference<ActivitiErrorMessageImpl>() {
            });
    }

    private ResponseEntity<CloudProcessInstance> createProcessWithoutCheck(String baseURL, StartProcessPayload payload) {
        return  testRestTemplate.exchange(baseURL,
            HttpMethod.POST,
            new HttpEntity<>(payload),
            new ParameterizedTypeReference<CloudProcessInstance>() {
            });
    }

    private ResponseEntity<CloudProcessInstance> startProcess(String baseURL, StartProcessPayload startProcess) {
        ResponseEntity<CloudProcessInstance> responseEntity = startProcessWithoutCheck(baseURL, startProcess);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }

    public ResponseEntity<CloudProcessInstance> startCreatedProcess(String processInstanceId) {
        String baseURL = PROCESS_INSTANCES_RELATIVE_URL.concat(processInstanceId).concat("/start");
        ResponseEntity<CloudProcessInstance> responseEntity = startCreatedProcessCall(baseURL);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        return responseEntity;
    }

    public ResponseEntity<ActivitiErrorMessageImpl> startCreatedProcessFailing(String processInstanceId) {
        String baseURL = PROCESS_INSTANCES_RELATIVE_URL.concat(processInstanceId).concat("/start");
        ResponseEntity<ActivitiErrorMessageImpl> responseEntity = startCreatedProcessCallFail(baseURL);
        return responseEntity;
    }

    private ResponseEntity<CloudProcessInstance> createProcess(String baseURL, StartProcessPayload startProcess) {
        ResponseEntity<CloudProcessInstance> responseEntity = createProcessWithoutCheck(baseURL, startProcess);

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

    public ResponseEntity<ActivitiErrorMessageImpl> startProcessWithErrorResponse(String baseURL,
                                                                                  StartProcessPayload startProcess) {
        return  testRestTemplate.exchange(baseURL,
                                          HttpMethod.POST,
                                          new HttpEntity<>(startProcess),
                                          new ParameterizedTypeReference<ActivitiErrorMessageImpl>() {
                                          });

    }

    public ResponseEntity<ActivitiErrorMessageImpl> startProcessWithErrorResponse(StartProcessPayload startProcess) {
        return  startProcessWithErrorResponse(PROCESS_INSTANCES_RELATIVE_URL, startProcess);
    }

    public ResponseEntity<ActivitiErrorMessageImpl> adminStartProcessWithErrorResponse(StartProcessPayload startProcess) {
        return  startProcessWithErrorResponse(PROCESS_INSTANCES_ADMIN_RELATIVE_URL, startProcess);
    }

    public ResponseEntity<PagedModel<CloudTask>> getTasks(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return getTasks(processInstanceEntity.getBody().getId());
    }

    public ResponseEntity<PagedModel<CloudTask>> getTasks(String processInstanceId) {
        ResponseEntity<PagedModel<CloudTask>> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/tasks",
                                                                                             HttpMethod.GET,
                                                                                             null,
                                                                                             new ParameterizedTypeReference<PagedModel<CloudTask>>() {
                                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CollectionModel<CloudVariableInstance>> getVariables(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        assertThat(processInstanceEntity.getBody()).isNotNull();
        return getVariables(processInstanceEntity.getBody().getId());
    }

    public ResponseEntity<CollectionModel<CloudVariableInstance>> getVariables(String processInstanceId) {
        ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = getVariablesNoReplyCheck(processInstanceId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CollectionModel<CloudVariableInstance>> getVariablesNoReplyCheck(String processInstanceId) {
        ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/variables",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<CollectionModel<CloudVariableInstance>>() {
                                                                                                    });
        return responseEntity;
    }

    public ResponseEntity<ActivitiErrorMessageImpl> callGetVariablesWithErrorResponse(String processInstanceId) {
        ResponseEntity<ActivitiErrorMessageImpl> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + processInstanceId + "/variables",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<ActivitiErrorMessageImpl>() {
                                                                                                    });
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

    public ResponseEntity<Void> setVariablesDoNotCheckStatus(String processInstanceId,
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
        return responseEntity;
    }

    public ResponseEntity<Void> setVariables(String processInstanceId,
                                             Map<String, Object> variables) {

        ResponseEntity<Void> responseEntity = setVariablesDoNotCheckStatus(processInstanceId,
                                                                           variables);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> adminSetVariablesDoNotCheckStatus(String processInstanceId,
                                                                  Map<String, Object> variables) {
        SetProcessVariablesPayload setProcessVariablesPayload = ProcessPayloadBuilder.setVariables()
                .withVariables(variables).build();

        HttpEntity<SetProcessVariablesPayload> requestEntity = new HttpEntity<>(
                setProcessVariablesPayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL
                        + processInstanceId + "/variables/",
                HttpMethod.PUT,
                requestEntity,
                new ParameterizedTypeReference<Void>() {
                });
       return responseEntity;
    }

    public ResponseEntity<Void> adminSetVariables(String processInstanceId,
                                                  Map<String, Object> variables) {
        ResponseEntity<Void> responseEntity = adminSetVariablesDoNotCheckStatus(processInstanceId,
                                                                                variables);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        return responseEntity;
    }

    public ResponseEntity<Void> adminRemoveVariables(String processId,
                                                     List<String> variableNames) {
        RemoveProcessVariablesPayload removeProcessVariablesPayload = ProcessPayloadBuilder.removeVariables()
                .withVariableNames(variableNames).build();

        HttpEntity<RemoveProcessVariablesPayload> requestEntity = new HttpEntity<>(
                removeProcessVariablesPayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(PROCESS_INSTANCES_ADMIN_RELATIVE_URL + processId + "/variables/",
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

    public ResponseEntity<PagedModel<ProcessInstance>> getSubprocesses(String processInstanceId) {
        ResponseEntity<PagedModel<ProcessInstance>> responseEntity = testRestTemplate.exchange(
                                                                                                   PROCESS_INSTANCES_RELATIVE_URL + processInstanceId+"/subprocesses",
                                                                                                   HttpMethod.GET,
                                                                                                   null,
                                                                                                   new ParameterizedTypeReference<PagedModel<ProcessInstance>>() {
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

    public ResponseEntity<PagedModel<ProcessInstance>> getPagedProcessInstances() {
        return getPagedProcessInstances(null);
    }

    public ResponseEntity<PagedModel<ProcessInstance>> getPagedProcessInstances(String pages) {
        return getPagedProcessInstances(PROCESS_INSTANCES_RELATIVE_URL,pages);
    }

    public ResponseEntity<PagedModel<ProcessInstance>> adminGetPagedProcessInstances() {
        return adminGetPagedProcessInstances(null);
    }

    public ResponseEntity<PagedModel<ProcessInstance>> adminGetPagedProcessInstances(String pages) {
        return getPagedProcessInstances(PROCESS_INSTANCES_ADMIN_RELATIVE_URL,pages);
    }

    public ResponseEntity<PagedModel<ProcessInstance>> getPagedProcessInstances(String baseURL,String pageFilters) {
        String pages = pageFilters != null ? pageFilters : "page=0&size=2";
        ResponseEntity<PagedModel<ProcessInstance>> responseEntity = testRestTemplate.exchange(
                                                                                        baseURL + "?" + pages,
                                                                                        HttpMethod.GET,
                                                                                        null,
                                                                                        new ParameterizedTypeReference<PagedModel<ProcessInstance>>() {
                                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }


}
