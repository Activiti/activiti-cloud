package org.activiti.cloud.acc.core.steps.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;

import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.TaskQueryService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;

@EnableRuntimeFeignContext
public class TaskQuerySteps {

    @Autowired
    private TaskQueryService taskQueryService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskQueryService.isServiceUp()).isTrue();
    }

    @Step
    public void checkTaskStatus(String taskId,
                                Task.TaskStatus expectedStatus) {

        await().untilAsserted(() -> assertThat(taskQueryService.queryTasksByIdAnsStatus(taskId,
                expectedStatus).getContent())
                .isNotNull()
                .isNotEmpty()
                .hasSize(1));
    }

    @Step
    public void checkSubtaskHasParentTaskId(String subtaskId,
                                            String parentTaskId) {

        await().untilAsserted(() -> {

            final Collection<CloudTask> tasks = taskQueryService.getTask(subtaskId).getContent();

            assertThat(tasks).isNotNull().isNotEmpty().hasSize(1).extracting(Task::getId,
                    Task::getParentTaskId).containsOnly(tuple(subtaskId,
                    parentTaskId));

        });


    }

    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return taskQueryService.getTasks();
    }

    @Step
    public CloudTask getTaskById(String id){
        return taskQueryService.getTask(id).getContent().iterator().next();
    }

    @Step
    public void checkTaskHasVariable(String taskId, String variableName, String variableValue) throws Exception {

        await().untilAsserted(() -> {

            assertThat(variableName).isNotNull();

            final Collection<CloudVariableInstance> variableInstances = taskQueryService.getTaskVariables(taskId).getContent();

            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();

            //one of the variables should have name matching variableName
            assertThat(variableInstances).extracting(VariableInstance::getName).contains(variableName);

            if(variableValue!=null){
                assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(tuple(variableName,variableValue));
            }

        });
    }

    @Step
    public PagedResources<CloudTask> getTasksByProcessInstance(String processInstanceId){
        return taskQueryService.getTasksByProcessInstance(processInstanceId);
    }
    
    @Step
    public PagedResources<CloudTask> getRootTasksByProcessInstance(String processInstanceId){
        return taskQueryService.getRootTasksByProcessInstance(processInstanceId);
    }
    
    @Step
    public PagedResources<CloudTask> getStandaloneTasks(){
        return taskQueryService.getStandaloneTasks();
    }

    @Step
    public PagedResources<CloudTask> getTasksByNameAndDescription(String taskName, String taskDescription){
        return taskQueryService.getTasksByNameAndDescription(taskName,
                                                                taskDescription);
    }

    @Step
    public Resources<CloudVariableInstance> getVariables(String taskId){
        return taskQueryService.getVariables(taskId);
    }
    
    
}
