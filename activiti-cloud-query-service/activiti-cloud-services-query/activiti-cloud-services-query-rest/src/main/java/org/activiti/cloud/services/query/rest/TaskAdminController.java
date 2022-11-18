/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.JsonViews;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/tasks", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
public class TaskAdminController {

    private final TaskRepository taskRepository;

    private TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private EntityFinder entityFinder;

    private TaskControllerHelper taskControllerHelper;

    @Autowired
    public TaskAdminController(
        TaskRepository taskRepository,
        TaskRepresentationModelAssembler taskRepresentationModelAssembler,
        EntityFinder entityFinder,
        TaskControllerHelper taskControllerHelper
    ) {
        this.taskRepository = taskRepository;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.entityFinder = entityFinder;
        this.taskControllerHelper = taskControllerHelper;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.GET, params = "!variableKeys")
    public PagedModel<EntityModel<QueryCloudTask>> findAll(
        @RequestParam(name = "rootTasksOnly", defaultValue = "false") Boolean rootTasksOnly,
        @RequestParam(name = "standalone", defaultValue = "false") Boolean standalone,
        @QuerydslPredicate(root = TaskEntity.class) Predicate predicate,
        VariableSearch variableSearch,
        Pageable pageable
    ) {
        return taskControllerHelper.findAll(
            predicate,
            variableSearch,
            pageable,
            Arrays.asList(new RootTasksFilter(rootTasksOnly), new StandAloneTaskFilter(standalone))
        );
    }

    @JsonView(JsonViews.ProcessVariables.class)
    @RequestMapping(method = RequestMethod.GET, params = "variableKeys")
    public PagedModel<EntityModel<QueryCloudTask>> findAllWithProcessVariables(
        @RequestParam(name = "rootTasksOnly", defaultValue = "false") Boolean rootTasksOnly,
        @RequestParam(name = "standalone", defaultValue = "false") Boolean standalone,
        @QuerydslPredicate(root = TaskEntity.class) Predicate predicate,
        @RequestParam(value = "variableKeys", required = false, defaultValue = "") List<String> processVariableKeys,
        VariableSearch variableSearch,
        Pageable pageable
    ) {
        return taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            Arrays.asList(new RootTasksFilter(rootTasksOnly), new StandAloneTaskFilter(standalone)),
            processVariableKeys
        );
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    public EntityModel<QueryCloudTask> findById(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(
            taskRepository,
            taskId,
            "Unable to find taskEntity for the given id:'" + taskId + "'"
        );

        return taskRepresentationModelAssembler.toModel(taskEntity);
    }

    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.GET)
    public List<String> getTaskCandidateUsers(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(
            taskRepository,
            taskId,
            "Unable to find taskEntity for the given id:'" + taskId + "'"
        );

        return taskEntity.getTaskCandidateUsers() != null
            ? taskEntity
                .getTaskCandidateUsers()
                .stream()
                .map(TaskCandidateUserEntity::getUserId)
                .collect(Collectors.toList())
            : null;
    }

    @RequestMapping(value = "/{taskId}/candidate-groups", method = RequestMethod.GET)
    public List<String> getTaskCandidateGroups(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(
            taskRepository,
            taskId,
            "Unable to find taskEntity for the given id:'" + taskId + "'"
        );

        return taskEntity.getTaskCandidateGroups() != null
            ? taskEntity
                .getTaskCandidateGroups()
                .stream()
                .map(TaskCandidateGroupEntity::getGroupId)
                .collect(Collectors.toList())
            : null;
    }
}
