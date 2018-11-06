package steps.runtime;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import rest.RuntimeDirtyContextHandler;
import rest.feign.EnableRuntimeFeignContext;
import services.runtime.ProcessRuntimeService;
import services.runtime.TaskRuntimeService;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
    public Collection<CloudTask> getTaskByProcessInstanceId(String processInstanceId) {
        return taskRuntimeService
                .getProcessInstanceTasks(processInstanceId).getContent();
    }

    @Step
    public void assignTaskToUser(String id,
                                 String user) {

        taskRuntimeService
                .assignTaskToUser(id,
                        user);
    }

    @Step
    public void cannotAssignTaskToUser(String id,
                                       String user){
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> {
                    taskRuntimeService
                            .assignTaskToUser(id,
                                    user);
                }).withMessageContaining("Unable to find task for the given id: " + id);
    }

    @Step
    public void completeTask(String id) {

        taskRuntimeService
                .completeTask(id);
    }

    @Step
    public void cannotCompleteTask(String id) {
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> {
                            taskRuntimeService
                                    .completeTask(id);
                        }
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
                taskRuntimeService.createNewTask(createTask));
    }

    public CloudTask createSubtask(String parentTaskId) {
        CreateTaskPayload subTask = TaskPayloadBuilder
                .create()
                .withName("subtask")
                .withDescription("subtask-description")
                .withAssignee("testuser")
                .build();
        return taskRuntimeService.createSubtask(parentTaskId,
                subTask);
    }

    public Resources<CloudTask> getSubtasks(String parentTaskId) {
        return taskRuntimeService.getSubtasks(parentTaskId);
    }

    @Step
    public CloudTask getTaskById(String id) {
        return taskRuntimeService.getTaskById(id);
    }

    @Step
    public void deleteTask(String taskId) {
        taskRuntimeService.deleteTask(taskId);
    }

    @Step
    public void checkTaskNotFound(String taskId) {
        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> taskRuntimeService.getTaskById(taskId)
        ).withMessageContaining("Unable to find task");
    }

    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return taskRuntimeService.getAllTasks();
    }

    @Step
    public void checkTaskStatus(String id, Task.TaskStatus status){
        //once a task is completed, it disappears from the runtime bundle
        if(!status.equals(Task.TaskStatus.COMPLETED)){
            assertThat(taskRuntimeService.getTaskById(id).getStatus()).isEqualTo(status);
        }
    }

}
