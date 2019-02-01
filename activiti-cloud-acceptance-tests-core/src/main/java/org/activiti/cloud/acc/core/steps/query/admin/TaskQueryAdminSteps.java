package org.activiti.cloud.acc.core.steps.query.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.admin.TaskQueryAdminService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

@EnableRuntimeFeignContext
public class TaskQueryAdminSteps {

    @Autowired
    private TaskQueryAdminService taskQueryAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskQueryAdminService.isServiceUp()).isTrue();
    }

 
    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return taskQueryAdminService.getTasks();
    }

    @Step
    public CloudTask getTaskById(String id){
        return taskQueryAdminService.getTask(id);
    }

    
    @Step
    public PagedResources<CloudTask> getRootTasksByProcessInstance(String processInstanceId){
        return taskQueryAdminService.getRootTasksByProcessInstance(processInstanceId);
    }
    
    @Step
    public PagedResources<CloudTask> getStandaloneTasks(){
        return taskQueryAdminService.getStandaloneTasks();
    }
    
    
}
