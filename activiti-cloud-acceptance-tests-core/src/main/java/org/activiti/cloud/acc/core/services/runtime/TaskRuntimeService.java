package org.activiti.cloud.acc.core.services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;

public interface TaskRuntimeService extends BaseService {

    String TASKS_PATH = "/v1/tasks/";

    @RequestLine("POST /v1/tasks/{id}/claim")
    @Headers("Content-Type: application/json")
    void claimTask(@Param("id") String id);

    @RequestLine("POST /v1/tasks/{id}/complete")
    @Headers("Content-Type: application/json")
    void completeTask(@Param("id") String id,
                      CompleteTaskPayload createTaskPayload);

    @RequestLine("POST /v1/tasks/")
    @Headers("Content-Type: application/json")
    CloudTask createTask(CreateTaskPayload task);

    @RequestLine("GET /v1/tasks/{id}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CloudTask getTask(@Param("id") String id);

    @RequestLine("DELETE /v1/tasks/{id}")
    void deleteTask(@Param("id") String id);

    @RequestLine("GET /v1/tasks/{parentTaskId}/subtasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    Resources<CloudTask> getSubtasks(@Param("parentTaskId") String parentTaskId);

    @RequestLine("GET /v1/tasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getTasks();

    @RequestLine("POST /v1/tasks/{taskId}/variables")
    @Headers("Content-Type: application/json")
    void createTaskVariable(@Param("taskId") String taskId, CreateTaskVariablePayload payload);

    @RequestLine("PUT /v1/tasks/{taskId}/variables/{variableName}")
    @Headers("Content-Type: application/json")
    void updateTaskVariable(@Param("taskId") String taskId,
                            @Param("variableName") String variableName,
                            UpdateTaskVariablePayload payload);


    @RequestLine("GET /v1/tasks/{id}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    Resources<CloudVariableInstance> getVariables(@Param("id") String id);

    @RequestLine("PUT /v1/tasks/{taskId}")
    @Headers("Content-Type: application/json")
    CloudTask updateTask(@Param("taskId") String taskId,
                         UpdateTaskPayload updateTaskPayload);
}
