package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.runtime.api.cmd.CompleteTask;
import org.activiti.runtime.api.cmd.CreateTask;
import org.activiti.runtime.api.cmd.UpdateTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/tasks", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources<TaskResource> getTasks(Pageable pageable);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    TaskResource getTaskById(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/claim", method = RequestMethod.POST)
    TaskResource claimTask(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/release", method = RequestMethod.POST)
    TaskResource releaseTask(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/complete", method = RequestMethod.POST)
    ResponseEntity<Void> completeTask(@PathVariable String taskId,
                                      @RequestBody(required = false) CompleteTask completeTask);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.DELETE)
    void deleteTask(@PathVariable String taskId);

    @RequestMapping(method = RequestMethod.POST)
    TaskResource createNewTask(@RequestBody CreateTask createTaskCmd);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.PUT)
    ResponseEntity<Void> updateTask(@PathVariable("taskId") String taskId,
            @RequestBody UpdateTask updateTaskCmd);

    @RequestMapping(value = "/{taskId}/subtask", method = RequestMethod.POST)
    TaskResource createSubtask(@PathVariable String taskId,
                                 @RequestBody CreateTask createSubtaskCmd);

    @RequestMapping(value = "/{taskId}/subtasks", method = RequestMethod.GET)
    Resources<TaskResource> getSubtasks(@PathVariable String taskId);
}
