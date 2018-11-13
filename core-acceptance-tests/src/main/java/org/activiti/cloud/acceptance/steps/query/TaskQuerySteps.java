package org.activiti.cloud.acceptance.steps.query;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.activiti.cloud.acceptance.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acceptance.services.query.TaskQueryService;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

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

}
