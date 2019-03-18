package org.activiti.cloud.services.rest.api;

import java.util.List;

import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/admin/v1/tasks", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskAdminController {

    @RequestMapping( method = RequestMethod.GET)
    PagedResources<Resource<CloudTask>> getTasks(Pageable pageable);
    
    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    Resource<CloudTask> getTaskById(@PathVariable String taskId);
    
    @RequestMapping(value = "/{taskId}", method = RequestMethod.PUT)
    Resource<CloudTask> updateTask(@PathVariable("taskId") String taskId,
                            @RequestBody UpdateTaskPayload updateTaskPayload);

    @RequestMapping(value = "/{taskId}/complete", method = RequestMethod.POST)
    Resource<CloudTask> completeTask(@PathVariable String taskId,
                              @RequestBody(required = false) CompleteTaskPayload completeTaskPayload);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.DELETE)
    Resource<CloudTask> deleteTask(@PathVariable String taskId);
    
    @RequestMapping(value = "/{taskId}/assign", method = RequestMethod.POST)
    Resource<CloudTask> assign(@PathVariable("taskId") String taskId,
                        @RequestBody AssignTaskPayload assignTaskPayload);

    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.POST)
    void addCandidateUsers(@PathVariable("taskId") String taskId,
                           @RequestBody CandidateUsersPayload candidateUsersPayload);
    
    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.DELETE)
    void deleteCandidateUsers(@PathVariable("taskId") String taskId,
                              @RequestBody CandidateUsersPayload candidateUsersPayload);
    
    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.GET)
    List<String> getUserCandidates(@PathVariable("taskId") String taskId);
    
    
    @RequestMapping(value = "/{taskId}/candidate-groups", method = RequestMethod.POST)
    void addCandidateGroups(@PathVariable("taskId") String taskId,
                            @RequestBody CandidateGroupsPayload candidateGroupsPayload);
    
    @RequestMapping(value = "/{taskId}/candidate-groups", method = RequestMethod.DELETE)
    void deleteCandidateGroups(@PathVariable("taskId") String taskId,
                               @RequestBody CandidateGroupsPayload candidateGroupsPayload);
    
       
    @RequestMapping(value = "/{taskId}/candidate-groups", method = RequestMethod.GET)
    List<String> getGroupCandidates(@PathVariable("taskId") String taskId);
}
