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

import java.util.Optional;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskVariableRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

@RestController
@RequestMapping(
        value = "/v1/tasks/{taskId}/variables",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskVariableController {

    private final TaskVariableRepository variableRepository;

    private TaskVariableRepresentationModelAssembler variableRepresentationModelAssembler;

    private AlfrescoPagedModelAssembler<TaskVariableEntity> pagedCollectionModelAssembler;


    @Autowired
    public TaskVariableController(TaskVariableRepository variableRepository,
                                  TaskVariableRepresentationModelAssembler variableRepresentationModelAssembler,
                                  AlfrescoPagedModelAssembler<TaskVariableEntity> pagedCollectionModelAssembler) {
        this.variableRepository = variableRepository;
        this.variableRepresentationModelAssembler = variableRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudVariableInstance>> getVariables(@PathVariable String taskId,
                                                                        @QuerydslPredicate(root = TaskVariableEntity.class) Predicate predicate,
                                                                        Pageable pageable) {

        predicate = Optional.ofNullable(predicate)
                            .orElseGet(BooleanBuilder::new);

        QTaskVariableEntity variable = QTaskVariableEntity.taskVariableEntity;

        BooleanExpression expression = variable.taskId.eq(taskId);

        if (predicate != null) {
            expression = expression.and(predicate);
        }

        Predicate extendedPredicated = expression;


        Page<TaskVariableEntity> variables = variableRepository.findAll(extendedPredicated,
                                                                    pageable);

        return pagedCollectionModelAssembler.toModel(pageable,
                                                  variables,
                                                  variableRepresentationModelAssembler);
    }

}
