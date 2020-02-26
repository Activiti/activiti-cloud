package org.activiti.cloud.acc.core.steps.runtime.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.TaskRuntimeAdminService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

@EnableRuntimeFeignContext
public class TaskRuntimeAdminSteps {

    @Autowired
    private TaskRuntimeAdminService taskRuntimeAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskRuntimeAdminService.isServiceUp()).isTrue();
    }

 
    @Step
    public void completeTask(String id, CompleteTaskPayload completeTaskPayload) {

        taskRuntimeAdminService
                .completeTask(id,completeTaskPayload);
    }

    @Step
    public void deleteTask(String taskId) {
        taskRuntimeAdminService.deleteTask(taskId);
    }

    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return taskRuntimeAdminService.getTasks();
    }

    @Step
    public CloudTask updateTask(String taskId, UpdateTaskPayload updateTaskPayload){
        return taskRuntimeAdminService.updateTask(
                taskId,
                updateTaskPayload);
    }
    
    @Step
    public CloudTask assignTask(String taskId, AssignTaskPayload assignTaskPayload){
        return taskRuntimeAdminService.assign(
                taskId,
                assignTaskPayload);
    }

}
