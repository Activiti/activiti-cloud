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

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.resources.VariableResource;
import org.activiti.cloud.services.query.rest.assembler.TaskVariableResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/admin/v1/tasks/{taskId}/variables",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskVariableAdminController {

    private AlfrescoPagedResourcesAssembler<TaskVariableEntity> pagedVariablesResourcesAssembler;

    private TaskVariableRepository variableRepository;

    private TaskVariableResourceAssembler variableResourceAssembler;
    

    @Autowired
    public TaskVariableAdminController(TaskVariableRepository variableRepository,
                                   TaskVariableResourceAssembler variableResourceAssembler,
                                   AlfrescoPagedResourcesAssembler<TaskVariableEntity> pagedVariablesResourcesAssembler) {
        this.variableRepository = variableRepository;
        this.variableResourceAssembler = variableResourceAssembler;
        this.pagedVariablesResourcesAssembler = pagedVariablesResourcesAssembler;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(IllegalStateException ex) {
        return ex.getMessage();
    }

    
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<VariableResource> getVariables(@PathVariable String taskId,
                                                         @QuerydslPredicate(root = TaskVariableEntity.class) Predicate predicate,
                                                         Pageable pageable) {

        QTaskVariableEntity variable = QTaskVariableEntity.taskVariableEntity;
        BooleanExpression expression = variable.taskId.eq(taskId);

        Predicate extendedPredicated = expression;
        if (predicate != null) {
            extendedPredicated = expression.and(predicate);
        }

         
        return pagedVariablesResourcesAssembler.toResource(pageable,
                                                           variableRepository.findAll(extendedPredicated,
                                                                                      pageable),
                                                           variableResourceAssembler); 
    }
    
}



