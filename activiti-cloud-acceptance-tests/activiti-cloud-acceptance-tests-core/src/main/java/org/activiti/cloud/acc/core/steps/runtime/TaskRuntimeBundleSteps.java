package org.activiti.cloud.acc.core.steps.runtime;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.stream.Collectors;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.TaskRuntimeService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;

import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestNotFoundErrorIsThrownBy;
import static org.assertj.core.api.Assertions.*;

@EnableRuntimeFeignContext
public class TaskRuntimeBundleSteps {

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private TaskRuntimeService taskRuntimeService;

    @Step
    public void checkServicesHealth() {
        assertThat(taskRuntimeService.isServiceUp()).isTrue();
    }

    @Step
    public void claimTask(String id) {

        taskRuntimeService
                .claimTask(id);
    }

    @Step
    public void cannotClaimTask(String id){
        assertThatRestNotFoundErrorIsThrownBy(
                () -> taskRuntimeService.claimTask(id)
        ).withMessageContaining("Unable to find task for the given id: " + id);
    }

    @Step
    public void completeTask(String id, CompleteTaskPayload completeTaskPayload) {
        taskRuntimeService
                .completeTask(id,completeTaskPayload);
    }

    @Step
    public void saveTask(String id, SaveTaskPayload saveTaskPayload) {
        taskRuntimeService
                .saveTask(id,saveTaskPayload);
    }
    @Step
    public void cannotCompleteTask(String id, CompleteTaskPayload createTaskPayload) {
        assertThatRestNotFoundErrorIsThrownBy(
                () -> taskRuntimeService.completeTask(id, createTaskPayload)
        ).withMessageContaining("Unable to find task for the given id: " + id);
    }

    @Step
    public CloudTask createNewTask() {

        CreateTaskPayload createTask = TaskPayloadBuilder
                .create()
                .withName("new-task")
                .withDescription("task-description")
                .withAssignee("testuser")
                .build();
        return dirtyContextHandler.dirty(
                taskRuntimeService.createTask(createTask));
    }

    @Step
    public CloudTask createNewUnassignedTask() {

        CreateTaskPayload createTask = TaskPayloadBuilder
                .create()
                .withName("unassigned-task")
                .withDescription("unassigned-task-description")
                .build();
        return dirtyContextHandler.dirty(
                taskRuntimeService.createTask(createTask));
    }

    public CloudTask createSubtask(String parentTaskId) {
        CreateTaskPayload subTask = TaskPayloadBuilder
                .create()
                .withName("subtask")
                .withDescription("subtask-description")
                .withAssignee("testuser")
                .withParentTaskId(parentTaskId)
                .build();
        return taskRuntimeService.createTask(subTask);
    }

    public CollectionModel<CloudTask> getSubtasks(String parentTaskId) {
        return taskRuntimeService.getSubtasks(parentTaskId);
    }

    @Step
    public CloudTask getTaskById(String id) {
        return taskRuntimeService.getTask(id);
    }

    @Step
    public void deleteTask(String taskId) {
        taskRuntimeService.deleteTask(taskId);
    }

    @Step
    public void checkTaskNotFound(String taskId) {
        assertThatRestNotFoundErrorIsThrownBy(
                () -> taskRuntimeService.getTask(taskId)
        ).withMessageContaining("Unable to find task");
    }

    @Step
    public PagedModel<CloudTask> getAllTasks(){
        return taskRuntimeService.getTasks();
    }

    @Step
    public void checkTaskStatus(String id, Task.TaskStatus status){
        //once a task is completed, it disappears from the runtime bundle
        if(!status.equals(Task.TaskStatus.COMPLETED)){
            assertThat(taskRuntimeService.getTask(id).getStatus()).isEqualTo(status);
        }
    }

    @Step
    public void updateVariable(String taskId, String name, Object value){

        taskRuntimeService.updateTaskVariable(taskId, name, TaskPayloadBuilder.updateVariable().withTaskId(taskId)
                .withVariable(name, value).build());
    }

    @Step
    public void createVariable(String taskId,
                               String name,
                               Object value) {

        taskRuntimeService.createTaskVariable(taskId,
                                              TaskPayloadBuilder
                                                      .createVariable()
                                                      .withTaskId(taskId)
                                                      .withVariable(name,
                                                                    value)
                                                      .build());
    }

    @Step
    public CollectionModel<CloudVariableInstance> getVariables(String taskId){
        return taskRuntimeService.getVariables(taskId);
    }

    @Step
    public CloudTask setTaskName(String taskId, String taskName){
        return taskRuntimeService.updateTask(
                taskId,
                TaskPayloadBuilder.update().withName(taskName).build());
    }

    @Step
    public CloudTask setTaskFormKey(String taskId, String formKey){
        return taskRuntimeService.updateTask(
                taskId,
                TaskPayloadBuilder.update().withFormKey(formKey).build());
    }

    @Step
    public CloudTask setTaskPriority(String taskId, int priority){
        return taskRuntimeService.updateTask(
                taskId,
                TaskPayloadBuilder.update().withPriority(priority).build());
    }

    @Step
    public CloudTask setTaskDueDate(String taskId, Date dueDate){
        return taskRuntimeService.updateTask(
                taskId,
                TaskPayloadBuilder.update().withDueDate(dueDate).build());
    }

    @Step
    public void releaseTask(String taskId){
        taskRuntimeService.releaseTask(taskId);
    }

    @Step
    public Collection<CloudTask> getTaskWithStandalone(boolean standalone) {
        return taskRuntimeService.getTasks().getContent().stream()
                .filter(cloudTask -> cloudTask.isStandalone() == standalone)
                .collect(Collectors.toSet());
    }
}
