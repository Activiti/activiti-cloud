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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Optional;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceVariableRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/v1/process-instances/{processInstanceId}",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class ProcessInstanceVariableController {

    private final VariableRepository variableRepository;

    private ProcessInstanceVariableRepresentationModelAssembler variableRepresentationModelAssembler;

    private AlfrescoPagedModelAssembler<ProcessVariableEntity> pagedCollectionModelAssembler;

    @Autowired
    public ProcessInstanceVariableController(
        ProcessInstanceVariableRepresentationModelAssembler variableRepresentationModelAssembler,
        VariableRepository variableRepository,
        AlfrescoPagedModelAssembler<ProcessVariableEntity> pagedCollectionModelAssembler
    ) {
        this.variableRepository = variableRepository;
        this.variableRepresentationModelAssembler = variableRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @RequestMapping(value = "/variables", method = RequestMethod.GET)
    public PagedModel<EntityModel<CloudVariableInstance>> getVariablesProcess(
        @PathVariable String processInstanceId,
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = ProcessVariableEntity.class
        ) Predicate predicate,
        Pageable pageable
    ) {
        predicate = Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new);

        QProcessVariableEntity variable = QProcessVariableEntity.processVariableEntity;

        //We will show only not deleted variables
        BooleanExpression expression = variable.processInstanceId.eq(processInstanceId);

        if (predicate != null) {
            expression = expression.and(predicate);
        }

        Predicate extendedPredicate = expression;

        Page<ProcessVariableEntity> variables = variableRepository.findAll(extendedPredicate, pageable);

        return pagedCollectionModelAssembler.toModel(pageable, variables, variableRepresentationModelAssembler);
    }
}
