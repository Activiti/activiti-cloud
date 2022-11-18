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
import java.util.ArrayList;
import java.util.Collection;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.JsonViews;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
    value = "/admin/v1/process-instances",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
public class ProcessInstanceDeleteController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    @Autowired
    public ProcessInstanceDeleteController(
        ProcessInstanceRepository processInstanceRepository,
        ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.DELETE)
    public CollectionModel<EntityModel<CloudProcessInstance>> deleteProcessInstances(
        @QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate
    ) {
        Collection<EntityModel<CloudProcessInstance>> result = new ArrayList<>();
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        for (ProcessInstanceEntity entity : iterable) {
            result.add(processInstanceRepresentationModelAssembler.toModel(entity));
        }

        processInstanceRepository.deleteAll(iterable);

        return CollectionModel.of(result);
    }
}
