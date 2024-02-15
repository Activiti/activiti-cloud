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

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
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
@Tag(name = "Process Instance Delete Controller")
public class ProcessInstanceDeleteController {

    private final ProcessInstanceRepository processInstanceRepository;

    private final TaskRepository taskRepository;

    private final VariableRepository variableRepository;

    private final ServiceTaskRepository serviceTaskRepository;

    private final BPMNActivityRepository bpmnActivityRepository;

    private final BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    @Autowired
    public ProcessInstanceDeleteController(
        ProcessInstanceRepository processInstanceRepository,
        TaskRepository taskRepository,
        VariableRepository variableRepository,
        ServiceTaskRepository serviceTaskRepository,
        BPMNActivityRepository bpmnActivityRepository,
        BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
        ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.variableRepository = variableRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.bpmnActivityRepository = bpmnActivityRepository;
        this.bpmnSequenceFlowRepository = bpmnSequenceFlowRepository;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.DELETE)
    @Transactional
    public CollectionModel<EntityModel<CloudProcessInstance>> deleteProcessInstances(
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = ProcessInstanceEntity.class
        ) Predicate predicate
    ) {
        Collection<EntityModel<CloudProcessInstance>> result = new ArrayList<>();
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        for (ProcessInstanceEntity entity : iterable) {
            Optional.ofNullable(entity.getTasks()).ifPresent(taskRepository::deleteAll);
            Optional.ofNullable(entity.getVariables()).ifPresent(variableRepository::deleteAll);
            Optional.ofNullable(entity.getServiceTasks()).ifPresent(serviceTaskRepository::deleteAll);
            Optional.ofNullable(entity.getActivities()).ifPresent(bpmnActivityRepository::deleteAll);
            Optional.ofNullable(entity.getSequenceFlows()).ifPresent(bpmnSequenceFlowRepository::deleteAll);

            result.add(processInstanceRepresentationModelAssembler.toModel(entity));
        }

        processInstanceRepository.deleteAll(iterable);

        return CollectionModel.of(result);
    }
}
