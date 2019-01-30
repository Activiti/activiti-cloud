/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.rest;

import java.util.List;
import java.util.stream.Collectors;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.resources.TaskResource;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.activiti.cloud.services.security.ActivitiForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/admin/v1/tasks",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskAdminController {

    private final TaskRepository taskRepository;

    private TaskResourceAssembler taskResourceAssembler;

    private AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler;

    private EntityFinder entityFinder;

    @Autowired
    public TaskAdminController(TaskRepository taskRepository,
                               TaskResourceAssembler taskResourceAssembler,
                               AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler,
                               EntityFinder entityFinder) {
        this.taskRepository = taskRepository;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.entityFinder = entityFinder;
    }

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }


    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(IllegalStateException ex) {
        return ex.getMessage();
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<TaskResource> findAll(@RequestParam(name = "rootTasksOnly", defaultValue = "false") Boolean rootTasksOnly,
                                                @RequestParam(name = "standalone", defaultValue = "false") Boolean standalone,
                                                @QuerydslPredicate(root = TaskEntity.class) Predicate predicate,
                                                Pageable pageable) {
        Predicate extendedPredicate=predicate;
        if (rootTasksOnly) {
            BooleanExpression parentTaskNull = QTaskEntity.taskEntity.parentTaskId.isNull(); 
            extendedPredicate= extendedPredicate !=null ? parentTaskNull.and(extendedPredicate) : parentTaskNull;
        }
        if (standalone) {
            BooleanExpression processInstanceIdNull = QTaskEntity.taskEntity.processInstanceId.isNull(); 
            extendedPredicate= extendedPredicate !=null ? processInstanceIdNull.and(extendedPredicate) : processInstanceIdNull;
        }
        
        Page<TaskEntity> page = taskRepository.findAll(extendedPredicate,
                                                       pageable);

        return pagedResourcesAssembler.toResource(pageable,
                                                  page,
                                                  taskResourceAssembler);
    }

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    public TaskResource findById(@PathVariable String taskId) {

        TaskEntity taskEntity = entityFinder.findById(taskRepository,
                                                      taskId,
                                                      "Unable to find taskEntity for the given id:'" + taskId + "'");

        return taskResourceAssembler.toResource(taskEntity);
    }
    
    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.GET)
    public List<String> getTaskCandidateUsers(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(taskRepository,
                                                      taskId,
                                                      "Unable to find taskEntity for the given id:'" + taskId + "'");

        List<String> candidateUsers = taskEntity.getTaskCandidateUsers()!=null ? 
                                      taskEntity.getTaskCandidateUsers().stream().map(it -> it.getUserId()).collect(Collectors.toList()) : 
                                      null;
        return candidateUsers;
    }
    
    @RequestMapping(value = "/{taskId}/candidate-groups", method = RequestMethod.GET)
    public List<String> getTaskCandidateGroups(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(taskRepository,
                                                      taskId,
                                                      "Unable to find taskEntity for the given id:'" + taskId + "'");

        List<String> candidateGroups = taskEntity.getTaskCandidateGroups()!=null ? 
                                       taskEntity.getTaskCandidateGroups().stream().map(it -> it.getGroupId()).collect(Collectors.toList()) : 
                                       null;
        return candidateGroups;
    }

}
