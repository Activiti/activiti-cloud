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

import java.util.Map;

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.api.resources.assembler.TaskResourceAssembler;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.rest.api.TaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskControllerImpl implements TaskController {

    private ProcessEngineWrapper processEngine;

    private final TaskResourceAssembler taskResourceAssembler;

    private AuthenticationWrapper authenticationWrapper;

    @Autowired
    public TaskControllerImpl(ProcessEngineWrapper processEngine,
                              TaskResourceAssembler taskResourceAssembler,
                              AuthenticationWrapper authenticationWrapper) {
        this.authenticationWrapper = authenticationWrapper;
        this.processEngine = processEngine;
        this.taskResourceAssembler = taskResourceAssembler;
    }

    @Override
    public PagedResources<TaskResource> getTasks(Pageable pageable,
                                                 PagedResourcesAssembler<Task> pagedResourcesAssembler) {
        Page<Task> page = processEngine.getTasks(pageable);
        return pagedResourcesAssembler.toResource(page,
                                                  taskResourceAssembler);
    }

    @Override
    public Resource<Task> getTaskById(@PathVariable String taskId) {
        return taskResourceAssembler.toResource(processEngine.getTaskById(taskId));
    }

    @Override
    public Resource<Task> claimTask(@PathVariable String taskId) {
        String assignee = authenticationWrapper.getAuthenticatedUserId();
        if (assignee == null) {
            throw new IllegalStateException("Assignee must be resolved from the Identity/Security Layer");
        }

        return taskResourceAssembler.toResource(processEngine.claimTask(new ClaimTaskCmd(taskId,
                                                                                         assignee)));
    }

    @Override
    public Resource<Task> releaseTask(@PathVariable String taskId) {

        return taskResourceAssembler.toResource(processEngine.releaseTask(new ReleaseTaskCmd(taskId)));
    }

    @Override
    public ResponseEntity<Void> completeTask(@PathVariable String taskId,
                                             @RequestBody(required = false) CompleteTaskCmd completeTaskCmd) {
        Map<String, Object> outputVariables = null;
        if (completeTaskCmd != null) {
            outputVariables = completeTaskCmd.getOutputVariables();
        }
        processEngine.completeTask(new CompleteTaskCmd(taskId,
                                                       outputVariables));
        return new ResponseEntity<>(HttpStatus.OK);
    }


    public AuthenticationWrapper getAuthenticationWrapper() {
        return authenticationWrapper;
    }
    
    public void setAuthenticationWrapper(AuthenticationWrapper authenticationWrapper) {
        this.authenticationWrapper = authenticationWrapper;
    }
}
