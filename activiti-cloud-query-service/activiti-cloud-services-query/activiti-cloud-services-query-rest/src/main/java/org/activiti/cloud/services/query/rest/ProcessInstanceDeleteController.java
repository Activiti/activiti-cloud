/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.activiti.cloud.api.process.model.QueryCloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.JsonViews;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
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

    private final TaskRepository taskRepository;

    private final VariableRepository variableRepository;

    private final ServiceTaskRepository serviceTaskRepository;

    private final BPMNActivityRepository bpmnActivityRepository;

    private final BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    private final TaskCandidateUserRepository taskCandidateUserRepository;

    private final TaskCandidateGroupRepository taskCandidateGroupRepository;

    private final TaskVariableRepository taskVariableRepository;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    @Autowired
    public ProcessInstanceDeleteController(
        ProcessInstanceRepository processInstanceRepository,
        TaskRepository taskRepository,
        VariableRepository variableRepository,
        ServiceTaskRepository serviceTaskRepository,
        BPMNActivityRepository bpmnActivityRepository,
        BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
        TaskCandidateUserRepository taskCandidateUserRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository,
        TaskVariableRepository taskVariableRepository,
        ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.variableRepository = variableRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.bpmnActivityRepository = bpmnActivityRepository;
        this.bpmnSequenceFlowRepository = bpmnSequenceFlowRepository;
        this.taskCandidateUserRepository = taskCandidateUserRepository;
        this.taskCandidateGroupRepository = taskCandidateGroupRepository;
        this.taskVariableRepository = taskVariableRepository;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.DELETE)
    @Transactional
    public CollectionModel<EntityModel<QueryCloudProcessInstance>> deleteProcessInstances(
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = ProcessInstanceEntity.class
        ) Predicate predicate
    ) {
        List<ProcessInstanceEntity> processInstances = StreamSupport.stream(
            processInstanceRepository.findAll(predicate).spliterator(),
            false
        ).toList();
        Set<String> processInstanceIds = processInstances
            .stream()
            .map(ProcessInstanceEntity::getId)
            .collect(Collectors.toSet());

        Collection<EntityModel<QueryCloudProcessInstance>> result = new ArrayList<>();
        for (ProcessInstanceEntity entity : processInstances) {
            result.add(processInstanceRepresentationModelAssembler.toModel(entity));
        }

        if (!processInstanceIds.isEmpty()) {
            deleteRelatedTasks(processInstanceIds);
            variableRepository.deleteAll(variableRepository.findByProcessInstanceIdIn(processInstanceIds));
            serviceTaskRepository.deleteAll(serviceTaskRepository.findByProcessInstanceIdIn(processInstanceIds));
            bpmnActivityRepository.deleteAll(bpmnActivityRepository.findByProcessInstanceIdIn(processInstanceIds));
            bpmnSequenceFlowRepository.deleteAll(
                bpmnSequenceFlowRepository.findByProcessInstanceIdIn(processInstanceIds)
            );
        }

        processInstanceRepository.deleteAll(processInstances);

        return CollectionModel.of(result);
    }

    /**
     * Deletes child rows via fresh queries (by processInstanceId / taskId) rather than by navigating the
     * process instance's lazy collections. Initializing a collection and then deleting its managed elements
     * leaves dangling references in the collection, which triggers a TransientPropertyValueException on flush.
     * Nothing lazy is serialized under {@link JsonViews.General}, so the collections are never touched.
     */
    private void deleteRelatedTasks(Set<String> processInstanceIds) {
        List<TaskEntity> tasks = taskRepository.findByProcessInstanceIdIn(processInstanceIds);
        if (tasks.isEmpty()) {
            return;
        }

        Set<String> taskIds = tasks.stream().map(TaskEntity::getId).collect(Collectors.toSet());
        taskCandidateUserRepository.deleteAll(taskCandidateUserRepository.findByTaskIdIn(taskIds));
        taskCandidateGroupRepository.deleteAll(taskCandidateGroupRepository.findByTaskIdIn(taskIds));
        taskVariableRepository.deleteAll(taskVariableRepository.findByTaskIdIn(taskIds));
        taskRepository.deleteAll(tasks);
    }
}
