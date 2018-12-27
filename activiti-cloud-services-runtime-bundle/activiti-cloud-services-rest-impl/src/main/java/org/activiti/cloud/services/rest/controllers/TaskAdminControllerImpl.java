/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import java.util.List;

import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskAdminController;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskAdminControllerImpl implements TaskAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final TaskResourceAssembler taskResourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public TaskAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                   TaskResourceAssembler taskResourceAssembler,
                                   AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler,
                                   SpringPageConverter pageConverter) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<TaskResource> getAllTasks(Pageable pageable) {
        Page<Task> tasksPage = taskAdminRuntime.tasks(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             tasksPage),
                                                  taskResourceAssembler);
    }
    
    public TaskResource assign(@PathVariable String taskId,
                               @RequestBody AssignTaskPayload assignTaskPayload) {
        if (assignTaskPayload!=null)
            assignTaskPayload.setTaskId(taskId);
 
        return taskResourceAssembler.toResource(taskAdminRuntime.assign(assignTaskPayload));
    }
    
    @Override
    public void addCandidateUsers(@PathVariable String taskId,
                                  @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null)
            candidateUsersPayload.setTaskId(taskId);
        
        taskAdminRuntime.addCandidateUsers(candidateUsersPayload);
    }
    
    @Override
    public void deleteCandidateUsers(@PathVariable String taskId,
                                     @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null)
            candidateUsersPayload.setTaskId(taskId);
        
        taskAdminRuntime.deleteCandidateUsers(candidateUsersPayload);
        
    }
    
    @Override
    public List<String> getUserCandidates(@PathVariable String taskId) {   
        return taskAdminRuntime.userCandidates(taskId);
    }
    
    @Override
    public void addCandidateGroups(@PathVariable String taskId,
                                   @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null)
            candidateGroupsPayload.setTaskId(taskId);
        
        taskAdminRuntime.addCandidateGroups(candidateGroupsPayload);
    }
    
    @Override
    public void deleteCandidateGroups(@PathVariable String taskId,
                                      @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null)
            candidateGroupsPayload.setTaskId(taskId);
        
        taskAdminRuntime.deleteCandidateGroups(candidateGroupsPayload);
    }
    
    @Override
    public List<String> getGroupCandidates(@PathVariable String taskId) {   
        return taskAdminRuntime.groupCandidates(taskId);
    }

}
