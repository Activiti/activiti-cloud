package org.activiti.cloud.acc.core.services.query; 

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;

public interface TaskQueryService extends BaseService {

    @RequestLine("GET /v1/tasks?status={status}&id={taskId}")
    PagedResources<CloudTask> queryTasksByIdAnsStatus(@Param("taskId") String taskId,
                                                      @Param("status") Task.TaskStatus taskStatus);

    @RequestLine("GET /v1/tasks?id={taskId}")
    PagedResources<CloudTask> getTask(@Param("taskId") String taskId);

    @RequestLine("GET /v1/tasks?sort=createdDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudTask> getTasks();
    
    @RequestLine("GET /v1/tasks/{taskId}/variables")
    @Headers("Content-Type: application/json")
    PagedResources<CloudVariableInstance> getTaskVariables(@Param("taskId") String taskId);

    @RequestLine("GET /v1/process-instances/{processInstanceId}/tasks")
    @Headers("Content-Type: application/json")
    PagedResources<CloudTask> getTasksByProcessInstance(@Param("processInstanceId") String processInstanceId);
    
    @RequestLine("GET /v1/tasks?rootTasksOnly=true&processInstanceId={processInstanceId}&sort=createdDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudTask> getRootTasksByProcessInstance(@Param("processInstanceId") String processInstanceId);
    
    @RequestLine("GET /v1/tasks?standalone=true&sort=createdDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getStandaloneTasks();

    @RequestLine("GET /v1/tasks?name={taskName}&description={taskDescription}")
    PagedResources<CloudTask> getTasksByNameAndDescription(@Param("taskName") String taskName,
                                                            @Param("taskDescription") String taskDescription);

    @RequestLine("GET /v1/tasks/{id}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    Resources<CloudVariableInstance> getVariables(@Param("id") String id);

}
