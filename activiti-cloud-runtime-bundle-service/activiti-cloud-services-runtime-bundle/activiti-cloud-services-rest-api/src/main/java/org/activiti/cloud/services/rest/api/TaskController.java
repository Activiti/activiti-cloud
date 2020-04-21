package org.activiti.cloud.services.rest.api;

import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@RequestMapping(value = "/v1/tasks", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskController {

    @RequestMapping(method = RequestMethod.GET)
    PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    EntityModel<CloudTask> getTaskById(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/claim", method = RequestMethod.POST)
    EntityModel<CloudTask> claimTask(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/release", method = RequestMethod.POST)
    EntityModel<CloudTask> releaseTask(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/complete", method = RequestMethod.POST)
    EntityModel<CloudTask> completeTask(@PathVariable String taskId,
                                      @RequestBody(required = false) CompleteTaskPayload completeTaskPayload);

    @RequestMapping(value = "/{taskId}/save", method = RequestMethod.POST)
    void saveTask(@PathVariable String taskId,
                  @RequestBody(required=true) SaveTaskPayload saveTaskPayload);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.DELETE)
    EntityModel<CloudTask> deleteTask(@PathVariable String taskId);

    @RequestMapping(method = RequestMethod.POST)
    EntityModel<CloudTask> createNewTask(@RequestBody CreateTaskPayload createTaskPayload);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.PUT)
    EntityModel<CloudTask> updateTask(@PathVariable("taskId") String taskId,
                                    @RequestBody UpdateTaskPayload updateTaskPayload);

    @RequestMapping(value = "/{taskId}/subtasks", method = RequestMethod.GET)
    PagedModel<EntityModel<CloudTask>> getSubtasks(Pageable pageable, @PathVariable String taskId);
}
