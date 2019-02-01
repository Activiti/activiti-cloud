package org.activiti.cloud.acc.core.services.query.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;

public interface TaskQueryAdminService extends BaseService {
    
    @RequestLine("GET /admin/v1/tasks/{taskId}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CloudTask getTask(@Param("taskId") String taskId);

    @RequestLine("GET /admin/v1/tasks?sort=createdDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getTasks();
    
    @RequestLine("GET /admin/v1/tasks?rootTasksOnly=true&processInstanceId={processInstanceId}&sort=createdDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudTask> getRootTasksByProcessInstance(@Param("processInstanceId") String processInstanceId);
    
    @RequestLine("GET /admin/v1/tasks?standalone=true&sort=createdDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getStandaloneTasks();
}
