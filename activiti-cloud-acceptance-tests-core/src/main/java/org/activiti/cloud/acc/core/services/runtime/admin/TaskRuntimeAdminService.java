package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;

public interface TaskRuntimeAdminService extends BaseService {
    
    @RequestLine("POST /admin/v1/tasks/{id}/complete")
    @Headers("Content-Type: application/json")
    void completeTask(@Param("id") String id,
                      CompleteTaskPayload createTaskPayload);
    
    @RequestLine("DELETE /admin/v1/tasks/{id}")
    void deleteTask(@Param("id") String id);
    
    @RequestLine("PUT /admin/v1/tasks/{taskId}")
    @Headers("Content-Type: application/json")
    CloudTask updateTask(@Param("taskId") String taskId,
                         UpdateTaskPayload updateTaskPayload);
    
    @RequestLine("POST /admin/v1/{taskId}/assign")
    @Headers("Content-Type: application/json")
    CloudTask assign(@Param("taskId") String taskId,
                     AssignTaskPayload assignTaskPayload);
    
    @RequestLine("GET /admin/v1/tasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getTasks();
}
