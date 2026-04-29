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

import static org.activiti.cloud.services.query.model.QBPMNActivityEntity.bPMNActivityEntity;
import static org.activiti.cloud.services.query.model.QTaskVariableEntity.taskVariableEntity;

import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessModelRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.rest.assembler.BPMNActivityRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/v1/process-instances/{processInstanceId}/diagram")
public class ProcessInstanceDiagramAdminController extends ProcessInstanceDiagramControllerBase {

    private final BPMNActivityRepresentationModelAssembler activityRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<BPMNActivityEntity> pagedActivitiesCollectionModelAssembler;

    private final TaskRepository taskRepository;

    private final TaskVariableRepository taskVariableRepository;

    @Autowired
    public ProcessInstanceDiagramAdminController(
        ProcessModelRepository processModelRepository,
        BPMNSequenceFlowRepository bpmnSequenceFlowRepository,
        ProcessDiagramGeneratorWrapper processDiagramGenerator,
        ProcessInstanceRepository processInstanceRepository,
        BPMNActivityRepository bpmnActivityRepository,
        EntityFinder entityFinder,
        BPMNActivityRepresentationModelAssembler activityRepresentationModelAssembler,
        AlfrescoPagedModelAssembler<BPMNActivityEntity> pagedActivitiesCollectionModelAssembler,
        TaskRepository taskRepository,
        TaskVariableRepository taskVariableRepository
    ) {
        super(
            processModelRepository,
            bpmnSequenceFlowRepository,
            processDiagramGenerator,
            processInstanceRepository,
            bpmnActivityRepository,
            entityFinder
        );
        this.activityRepresentationModelAssembler = activityRepresentationModelAssembler;
        this.pagedActivitiesCollectionModelAssembler = pagedActivitiesCollectionModelAssembler;
        this.taskRepository = taskRepository;
        this.taskVariableRepository = taskVariableRepository;
    }

    @GetMapping(produces = IMAGE_SVG_XML)
    @ResponseBody
    public String getProcessDiagramAdmin(@PathVariable String processInstanceId) {
        return generateDiagram(processInstanceId);
    }

    @Operation(
        summary = "Get the per-activity execution timeline for a process instance",
        description = "Returns every BPMN activity (user task, service task, start/end event, gateway, boundary event...) " +
        "that has been visited by the given process instance, regardless of status."
    )
    @GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public PagedModel<EntityModel<CloudBPMNActivity>> getProcessDiagramActivitiesAdmin(
        @PathVariable String processInstanceId,
        @Parameter(
            description = RestDocConstants.PREDICATE_DESC,
            example = RestDocConstants.PREDICATE_EXAMPLE
        ) @QuerydslPredicate(root = BPMNActivityEntity.class) Predicate predicate,
        Pageable pageable
    ) {
        Predicate filter = bPMNActivityEntity.processInstanceId.eq(processInstanceId).and(predicate);

        Page<BPMNActivityEntity> page = bpmnActivityRepository.findAll(filter, pageable);
        return pagedActivitiesCollectionModelAssembler.toModel(pageable, page, activityRepresentationModelAssembler);
    }

    @Operation(
        summary = "Get task-scoped variable snapshots for a process instance diagram",
        description = "Returns variables grouped by their task's BPMN element id (taskDefinitionKey). " +
        "Each entry represents the variable values as they were set in that specific task, " +
        "enabling per-activity variable tracking on the diagram."
    )
    @GetMapping(
        value = "/variables",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    public List<ActivityVariableSnapshot> getProcessDiagramVariablesAdmin(
        @PathVariable String processInstanceId
    ) {
        Map<String, String> taskIdToActivityId = new LinkedHashMap<>();
        Page<TaskEntity> tasks = taskRepository.findAll(
            QTaskEntity.taskEntity.processInstanceId.eq(processInstanceId),
            PageRequest.of(0, 1000)
        );
        for (TaskEntity task : tasks) {
            if (task.getId() != null && task.getTaskDefinitionKey() != null) {
                taskIdToActivityId.put(task.getId(), task.getTaskDefinitionKey());
            }
        }

        if (taskIdToActivityId.isEmpty()) {
            return List.of();
        }

        Page<TaskVariableEntity> taskVariables = taskVariableRepository.findAll(
            taskVariableEntity.processInstanceId.eq(processInstanceId),
            PageRequest.of(0, 5000)
        );

        Map<String, List<VariableEntry>> grouped = new LinkedHashMap<>();
        for (TaskVariableEntity variable : taskVariables) {
            String activityId = taskIdToActivityId.get(variable.getTaskId());
            if (activityId == null || variable.getName() == null) {
                continue;
            }
            grouped
                .computeIfAbsent(activityId, k -> new ArrayList<>())
                .add(new VariableEntry(variable.getName(), variable.getType(), serializeValue(variable.getValue())));
        }

        List<ActivityVariableSnapshot> result = new ArrayList<>();
        for (Map.Entry<String, List<VariableEntry>> entry : grouped.entrySet()) {
            result.add(new ActivityVariableSnapshot(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private String serializeValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    public record VariableEntry(String name, String type, String value) {}

    public record ActivityVariableSnapshot(String activityId, List<VariableEntry> variables) {}
}
