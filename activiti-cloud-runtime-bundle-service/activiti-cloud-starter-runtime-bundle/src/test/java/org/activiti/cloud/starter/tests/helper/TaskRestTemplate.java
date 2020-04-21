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

import java.util.Collections;
import java.util.List;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@TestComponent
public class TaskRestTemplate {

    private static final String TASK_VAR_RELATIVE_URL = "/v1/tasks/";
    private static final String ADMIN_TASK_VAR_RELATIVE_URL = "/admin/v1/tasks/";

    private static final ParameterizedTypeReference<CloudTask> TASK_RESPONSE_TYPE = new ParameterizedTypeReference<CloudTask>() {
    };
    private static final ParameterizedTypeReference<PagedModel<CloudTask>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedModel<CloudTask>>() {
    };
    private static final ParameterizedTypeReference<CollectionModel<EntityModel<CandidateUser>>> CANDIDATE_USERS_RESPONSE_TYPE = new ParameterizedTypeReference<CollectionModel<EntityModel<CandidateUser>>> () {
    };
    private static final ParameterizedTypeReference<CollectionModel<EntityModel<CandidateGroup>>> CANDIDATES_GROUPS_RESPONSE_TYPE = new ParameterizedTypeReference<CollectionModel<EntityModel<CandidateGroup>>> () {
    };
    private static final ParameterizedTypeReference<Void> VOID_RESPONSE_TYPE = new ParameterizedTypeReference<Void>() {
    };

    private TestRestTemplate testRestTemplate;

    public TaskRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    public ResponseEntity<CloudTask> complete(Task task) {
        return complete(task,
                        null);
    }

    public ResponseEntity<CloudTask> adminComplete(Task task) {
        return adminComplete(task,
                             null);
    }

    public ResponseEntity<CloudTask> complete(Task task,CompleteTaskPayload completeTaskPayload) {
        return complete(task,
                        TASK_VAR_RELATIVE_URL,
                        completeTaskPayload);
    }

    public ResponseEntity<CloudTask> adminComplete(Task task,CompleteTaskPayload completeTaskPayload) {
        return complete(task,
                        ADMIN_TASK_VAR_RELATIVE_URL,
                        completeTaskPayload);
    }


    private ResponseEntity<CloudTask> complete(Task task,
                                               String baseURL,
                                               CompleteTaskPayload completeTaskPayload
                                               ) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(baseURL + task.getId() + "/complete",
                                                                             HttpMethod.POST,
                                                                             completeTaskPayload!=null ?  new HttpEntity<>(completeTaskPayload) : null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        return responseEntity;
    }

    public ResponseEntity<CloudTask> delete(Task task) {
        return delete(task,
                      TASK_VAR_RELATIVE_URL);
    }

    public ResponseEntity<CloudTask> adminDelete(Task task) {
        return delete(task,
                      ADMIN_TASK_VAR_RELATIVE_URL);
    }

    private ResponseEntity<CloudTask> delete(Task task,
                                             String taskVarRelativeUrl) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(taskVarRelativeUrl + task.getId(),
                                                                             HttpMethod.DELETE,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        return responseEntity;
    }

    public ResponseEntity<CloudTask> claim(Task task) {
        return claim(task.getId());
    }

    public ResponseEntity<CloudTask> claim(String taskId) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId + "/claim",
                                                                             HttpMethod.POST,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CloudTask> release(Task task) {
        return release(task.getId());
    }

    public ResponseEntity<CloudTask> release(String taskId) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId + "/release",
                                                                             HttpMethod.POST,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CloudTask> adminAssignTask(AssignTaskPayload assignTask) {
        return testRestTemplate.exchange(ADMIN_TASK_VAR_RELATIVE_URL + assignTask.getTaskId() + "/assign",
                                         HttpMethod.POST,
                                         new HttpEntity<>(assignTask),
                                         TASK_RESPONSE_TYPE);
    }

    public ResponseEntity<CollectionModel<EntityModel<CandidateUser>>> getUserCandidates(String taskId) {
        ResponseEntity<CollectionModel<EntityModel<CandidateUser>>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId + "/candidate-users",
                                                                                                      HttpMethod.GET,
                                                                                                      null,
                                                                                                      CANDIDATE_USERS_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> addUserCandidates(CandidateUsersPayload candidateUsers) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateUsers.getTaskId()+"/candidate-users",
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(candidateUsers),
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> deleteUserCandidates(CandidateUsersPayload candidateUsers) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateUsers.getTaskId()+"/candidate-users",
                                                                             HttpMethod.DELETE,
                                                                             new HttpEntity<>(candidateUsers),
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<CollectionModel<EntityModel<CandidateGroup>>> getGroupCandidates(String taskId) {
        ResponseEntity<CollectionModel<EntityModel<CandidateGroup>>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + taskId + "/candidate-groups",
                                                                                                       HttpMethod.GET,
                                                                                                       null,
                                                                                                       CANDIDATES_GROUPS_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> addGroupCandidates(CandidateGroupsPayload candidateGroups) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateGroups.getTaskId()+"/candidate-groups",
                                                                             HttpMethod.POST,
                                                                             new HttpEntity<>(candidateGroups),
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> deleteGroupCandidates(CandidateGroupsPayload candidateGroups) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + candidateGroups.getTaskId()+"/candidate-groups",
                                                                             HttpMethod.DELETE,
                                                                             new HttpEntity<>(candidateGroups),
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public PagedModel<CloudTask> getSubTasks(CloudTask parentTask) {
        ResponseEntity<PagedModel<CloudTask>> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + parentTask.getId() + "/subtasks",
                                                                                             HttpMethod.GET,
                                                                                             null,
                                                                                             new ParameterizedTypeReference<PagedModel<CloudTask>>() {
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

    public ResponseEntity<PagedModel<CloudTask>> getTasks() {
        return getTasks(TASK_VAR_RELATIVE_URL);
    }

    public ResponseEntity<PagedModel<CloudTask>> adminGetTasks() {
        return getTasks(ADMIN_TASK_VAR_RELATIVE_URL);
    }

    private ResponseEntity<PagedModel<CloudTask>> getTasks(String baseURL) {
        return testRestTemplate.exchange(baseURL,
                                         HttpMethod.GET,
                                         null,
                                         PAGED_TASKS_RESPONSE_TYPE);
    }

    public ResponseEntity<CloudTask> getTask(String taskId) {
        return getTask(taskId,TASK_VAR_RELATIVE_URL);
    }

    public ResponseEntity<CloudTask> adminGetTask(String taskId) {
        return getTask(taskId,ADMIN_TASK_VAR_RELATIVE_URL);
    }

    private ResponseEntity<CloudTask> getTask(String taskId,String baseURL) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(baseURL + taskId,
                                                                             HttpMethod.GET,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public void updateTask(UpdateTaskPayload updateTask) {
        updateTask(updateTask,
                   TASK_VAR_RELATIVE_URL);
    }

    public void adminUpdateTask(UpdateTaskPayload updateTask) {
        updateTask(updateTask,
                   ADMIN_TASK_VAR_RELATIVE_URL);
    }

    private void updateTask(UpdateTaskPayload updateTask,
                            String baseURL) {
        updateTask(updateTask.getTaskId(),
                   updateTask,
                   baseURL);
    }

    public void updateTask(String taskId,UpdateTaskPayload updateTask) {
        updateTask(taskId,
                   updateTask,
                   TASK_VAR_RELATIVE_URL);
    }

    public void adminUpdateTask(String taskId,UpdateTaskPayload updateTask) {
        updateTask(taskId,
                   updateTask,
                   ADMIN_TASK_VAR_RELATIVE_URL);
    }

    private void updateTask(String taskId,
                            UpdateTaskPayload updateTask,
                            String baseURL) {
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(baseURL + taskId,
                                                                             HttpMethod.PUT,
                                                                             new HttpEntity<>(updateTask),
                                                                             TASK_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    public ResponseEntity<Void> createVariable(String taskId,
                                               String name,
                                               Object value) {
        CreateTaskVariablePayload createTaskVariablePayload = TaskPayloadBuilder.createVariable().withVariable(name,value).build();

        HttpEntity<CreateTaskVariablePayload> requestEntity = new HttpEntity<>(
                createTaskVariablePayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TaskRestTemplate.TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                        HttpMethod.POST,
                                                                        requestEntity,
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }


    public ResponseEntity<Void> updateVariable(String taskId,
                                               String name,
                                               Object value) {
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder.updateVariable().withVariable(name,value).build();

        HttpEntity<UpdateTaskVariablePayload> requestEntity = new HttpEntity<>(
                updateTaskVariablePayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TaskRestTemplate.TASK_VAR_RELATIVE_URL + taskId + "/variables/{variableName}",
                                                                        HttpMethod.PUT,
                                                                        requestEntity,
                                                                        VOID_RESPONSE_TYPE,
                                                                        name);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }


    public ResponseEntity<CollectionModel<CloudVariableInstance>> getVariables(String taskId) {

        ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = testRestTemplate.exchange(TaskRestTemplate.TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<CollectionModel<CloudVariableInstance>>() {
                                                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> adminCreateVariable(String taskId,
                                                    String name,
                                                    Object value) {
        CreateTaskVariablePayload createTaskVariablePayload = TaskPayloadBuilder.createVariable().withVariable(name,
                                                                                                               value).build();

        HttpEntity<CreateTaskVariablePayload> requestEntity = new HttpEntity<>(
                createTaskVariablePayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TaskRestTemplate.ADMIN_TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                        HttpMethod.POST,
                                                                        requestEntity,
                                                                        VOID_RESPONSE_TYPE);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> adminUpdateVariable(String taskId,
                                                    String name,
                                                    Object value) {
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder.updateVariable().withVariable(name,
                                                                                                               value).build();

        HttpEntity<UpdateTaskVariablePayload> requestEntity = new HttpEntity<>(
                updateTaskVariablePayload,
                null);
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TaskRestTemplate.ADMIN_TASK_VAR_RELATIVE_URL + taskId + "/variables/{variableName}",
                                                                        HttpMethod.PUT,
                                                                        requestEntity,
                                                                        VOID_RESPONSE_TYPE,
                                                                        name);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }


    public ResponseEntity<CollectionModel<CloudVariableInstance>> adminGetVariables(String taskId) {

        ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = testRestTemplate.exchange(TaskRestTemplate.ADMIN_TASK_VAR_RELATIVE_URL + taskId + "/variables/",
                                                                                                    HttpMethod.GET,
                                                                                                    null,
                                                                                                    new ParameterizedTypeReference<CollectionModel<CloudVariableInstance>>() {
                                                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    public ResponseEntity<Void> save(Task task, SaveTaskPayload saveTaskPayload ) {
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASK_VAR_RELATIVE_URL + task.getId() + "/save",
                                                                             HttpMethod.POST,
                                                                             saveTaskPayload!=null ?  new HttpEntity<>(saveTaskPayload) : null,
                                                                             VOID_RESPONSE_TYPE);

        return responseEntity;
    }


}
