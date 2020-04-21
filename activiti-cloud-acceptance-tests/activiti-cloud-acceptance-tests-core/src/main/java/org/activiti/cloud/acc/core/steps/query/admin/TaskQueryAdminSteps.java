package org.activiti.cloud.acc.core.steps.query.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.admin.TaskQueryAdminService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;

@EnableRuntimeFeignContext
public class TaskQueryAdminSteps {

    @Autowired
    private TaskQueryAdminService taskQueryAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskQueryAdminService.isServiceUp()).isTrue();
    }


    @Step
    public PagedModel<CloudTask> getAllTasks(){
        return taskQueryAdminService.getTasks();
    }

    @Step
    public CloudTask getTaskById(String id){
        return taskQueryAdminService.getTask(id);
    }


    @Step
    public PagedModel<CloudTask> getRootTasksByProcessInstance(String processInstanceId){
        return taskQueryAdminService.getRootTasksByProcessInstance(processInstanceId);
    }

    @Step
    public PagedModel<CloudTask> getStandaloneTasks(){
        return taskQueryAdminService.getStandaloneTasks();
    }

    @Step
    public PagedModel<CloudTask> getNonStandaloneTasks(){
        return taskQueryAdminService.getNonStandaloneTasks();
    }

    @Step
    public CollectionModel<EntityModel<CloudTask>> deleteTasks(){
        return taskQueryAdminService.deleteTasks();
    }

}
