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

import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_EXAMPLE;
import static org.activiti.cloud.services.query.rest.RestDocConstants.ROOT_TASKS_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.STANDALONE_TASKS_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.VARIABLE_KEYS_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.VARIABLE_KEYS_EXAMPLE;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.JsonViews;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.payload.TasksQueryBody;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Find tasks Admin", hidden = true)
    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.GET, params = "!variableKeys")
    public PagedModel<EntityModel<QueryCloudTask>> findAllServiceTaskAdmin(
        @Parameter(description = ROOT_TASKS_DESC) @RequestParam(
            name = "rootTasksOnly",
            defaultValue = "false"
        ) Boolean rootTasksOnly,
        @Parameter(description = STANDALONE_TASKS_DESC) @RequestParam(
            name = "standalone",
            defaultValue = "false"
        ) Boolean standalone,
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = TaskEntity.class
        ) Predicate predicate,
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

    @Operation(summary = "Find tasks with Process Variables Admin")
    @JsonView(JsonViews.ProcessVariables.class)
    @RequestMapping(method = RequestMethod.GET, params = "variableKeys")
    public PagedModel<EntityModel<QueryCloudTask>> findAllWithProcessVariablesAdmin(
        @Parameter(description = ROOT_TASKS_DESC) @RequestParam(
            name = "rootTasksOnly",
            defaultValue = "false"
        ) Boolean rootTasksOnly,
        @Parameter(description = STANDALONE_TASKS_DESC) @RequestParam(
            name = "standalone",
            defaultValue = "false"
        ) Boolean standalone,
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = TaskEntity.class
        ) Predicate predicate,
        @Parameter(description = VARIABLE_KEYS_DESC, example = VARIABLE_KEYS_EXAMPLE) @RequestParam(
            value = "variableKeys",
            required = false,
            defaultValue = ""
        ) List<String> processVariableKeys,
        VariableSearch variableSearch,
        Pageable pageable
    ) {
        return taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            Arrays.asList(new RootTasksFilter(rootTasksOnly), new StandAloneTaskFilter(standalone)),
            Collections.emptyList(),
            processVariableKeys.stream().map(k -> k.split("/")).map(s -> new ProcessVariableKey(s[0], s[1])).toList()
        );
    }

    @RequestMapping(method = RequestMethod.POST)
    public MappingJacksonValue findAllFromBodyTaskAdmin(
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = TaskEntity.class
        ) Predicate predicate,
        @RequestBody(required = false) TasksQueryBody payload,
        VariableSearch variableSearch,
        Pageable pageable
    ) {
        TasksQueryBody queryBody = Optional.ofNullable(payload).orElse(new TasksQueryBody());

        PagedModel<EntityModel<QueryCloudTask>> pagedModel = taskControllerHelper.findAllFromBody(
            predicate,
            variableSearch,
            pageable,
            Arrays.asList(
                new RootTasksFilter(queryBody.isRootTasksOnly()),
                new StandAloneTaskFilter(queryBody.isStandalone())
            ),
            queryBody
                .getVariableKeys()
                .stream()
                .map(k -> k.split("/"))
                .map(s -> new ProcessVariableKey(s[0], s[1]))
                .toList()
        );

        MappingJacksonValue result = new MappingJacksonValue(pagedModel);
        if (queryBody.hasVariableKeys()) {
            result.setSerializationView(JsonViews.ProcessVariables.class);
        } else {
            result.setSerializationView(JsonViews.General.class);
        }

        return result;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    public EntityModel<QueryCloudTask> findByIdTaskAdmin(@PathVariable String taskId) {
        TaskEntity taskEntity = entityFinder.findById(
            taskRepository,
            taskId,
            "Unable to find taskEntity for the given id:'" + taskId + "'"
        );

        return taskRepresentationModelAssembler.toModel(taskEntity);
    }

    @RequestMapping(value = "/{taskId}/candidate-users", method = RequestMethod.GET)
    public List<String> getTaskCandidateUsersAdmin(@PathVariable String taskId) {
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
    public List<String> getTaskCandidateGroupsAdmin(@PathVariable String taskId) {
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
