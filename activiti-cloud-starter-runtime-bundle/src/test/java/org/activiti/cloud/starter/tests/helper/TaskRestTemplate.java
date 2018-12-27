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

import java.util.List;
import java.util.Map;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SetTaskVariablesPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
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

@Component
public class TaskRestTemplate {

    private static final String TASK_VAR_RELATIVE_URL = "/v1/tasks/";
    private static final String ADMIN_TASK_VAR_RELATIVE_URL = "/admin/v1/tasks/";

    private static final ParameterizedTypeReference<CloudTask> TASK_RESPONSE_TYPE = new ParameterizedTypeReference<CloudTask>() {
    };

    @Autowired
    private TestRestTemplate testRestTemplate;

    public ResponseEntity<Task> complete(Task task) {
        ResponseEntity<Task> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + task.getId() + "/complete",
                                                                        HttpMethod.POST,
                                                                        null,
                                                                        new ParameterizedTypeReference<Task>() {
                                                                        });
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        return responseEntity;
    }

    public ResponseEntity<CloudTask> delete(Task task) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + task.getId(),
                                                                        HttpMethod.DELETE,
                                                                        null,
                                                                        TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        return responseEntity;
    }

    public ResponseEntity<CloudTask> claim(Task task) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + task.getId() + "/claim",
                                                                             HttpMethod.POST,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<CloudTask> release(Task task) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + task.getId() + "/release",
                                                                             HttpMethod.POST,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<CloudTask> adminAssignTask(AssignTaskPayload assigntask) {
        return testRestTemplate.exchange(ADMIN_TASK_VAR_RELATIVE_URL + assigntask.getTaskId() + "/assign",
                                         HttpMethod.POST,
                                         new HttpEntity<>(assigntask),
                                         TASK_RESPONSE_TYPE);
    }
    
    public ResponseEntity<List<String>> getUserCandidates(String taskId) {
        ResponseEntity<List<String>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId+"/candidate-users",
                                                                             HttpMethod.GET,
                                                                             null,
                                                                             new ParameterizedTypeReference<List<String>>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<Void> addUserCandidates(CandidateUsersPayload candidateusers) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateusers.getTaskId()+"/candidate-users",
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(candidateusers),
                                                                             new ParameterizedTypeReference<Void>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<Void> deleteUserCandidates(CandidateUsersPayload candidateusers) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateusers.getTaskId()+"/candidate-users",
                                                                             HttpMethod.DELETE,
                                                                             new HttpEntity<>(candidateusers),
                                                                             new ParameterizedTypeReference<Void>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<List<String>> getGroupCandidates(String taskId) {
        ResponseEntity<List<String>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId+"/candidate-groups",
                                                                             HttpMethod.GET,
                                                                             null,
                                                                             new ParameterizedTypeReference<List<String>>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<Void> addGroupCandidates(CandidateGroupsPayload candidategroups) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidategroups.getTaskId()+"/candidate-groups",
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(candidategroups),
                                                                             new ParameterizedTypeReference<Void>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public ResponseEntity<Void> deleteGroupCandidates(CandidateGroupsPayload candidategroups) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidategroups.getTaskId()+"/candidate-groups",
                                                                             HttpMethod.DELETE,
                                                                             new HttpEntity<>(candidategroups),
                                                                             new ParameterizedTypeReference<Void>() {
                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }
    
    public CloudTask createSubTask(CreateTaskPayload createTask) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + createTask.getParentTaskId() + "/subtask",
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(createTask),
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity.getBody();
    }

    public PagedResources<CloudTask> getSubTasks(CloudTask parentTask) {
        ResponseEntity<PagedResources<CloudTask>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + parentTask.getId() + "/subtasks",
                                                                                             HttpMethod.GET,
                                                                                             null,
                                                                                             new ParameterizedTypeReference<PagedResources<CloudTask>>() {
                                                                                             });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity.getBody();
    }

    public CloudTask createTask(CreateTaskPayload createTask) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL,
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(createTask),
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity.getBody();
    }

    public ResponseEntity<CloudTask> getTask(String taskId) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId,
                                                                             HttpMethod.GET,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public void updateTask(UpdateTaskPayload updateTask) {
        ResponseEntity<Task> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + updateTask.getTaskId(),
                                                                        HttpMethod.PUT,
                                                                        new HttpEntity<>(updateTask),
                                                                        new ParameterizedTypeReference<Task>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    public ResponseEntity<Void> setVariables(String taskId,
                                             Map<String, Object> variables) {
        SetTaskVariablesPayload setTaskVariablesPayload = TaskPayloadBuilder.setVariables().withTaskId(taskId).withVariables(variables).build();

        HttpEntity<SetTaskVariablesPayload> requestEntity = new HttpEntity<>(
                setTaskVariablesPayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TaskRestTemplate.TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                        HttpMethod.POST,
                                                                        requestEntity,
                                                                        new ParameterizedTypeReference<Void>() {
                                                                        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }


    public ResponseEntity<Resources<CloudVariableInstance>> getVariables(String taskId) {

        ResponseEntity<Resources<CloudVariableInstance>> responseEntity = testRestTemplate.exchange(TaskRestTemplate.TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<Resources<CloudVariableInstance>>() {
                                                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

}
