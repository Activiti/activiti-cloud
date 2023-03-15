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
package org.activiti.cloud.services.rest.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.services.rest.api.CandidateGroupController;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CandidateGroupControllerImpl implements CandidateGroupController {

    private final TaskRuntime taskRuntime;

    private final CollectionModelAssembler resourcesAssembler;

    private final GroupCandidatesRepresentationModelAssembler groupCandidatesRepresentationModelAssembler;

    private final ToCandidateGroupConverter toCandidateGroupConverter;

    public CandidateGroupControllerImpl(
        TaskRuntime taskRuntime,
        CollectionModelAssembler resourcesAssembler,
        GroupCandidatesRepresentationModelAssembler groupCandidatesRepresentationModelAssembler,
        ToCandidateGroupConverter toCandidateGroupConverter
    ) {
        this.taskRuntime = taskRuntime;
        this.resourcesAssembler = resourcesAssembler;
        this.groupCandidatesRepresentationModelAssembler = groupCandidatesRepresentationModelAssembler;
        this.toCandidateGroupConverter = toCandidateGroupConverter;
    }

    @Override
    public void addCandidateGroups(
        @PathVariable String taskId,
        @RequestBody CandidateGroupsPayload candidateGroupsPayload
    ) {
        if (candidateGroupsPayload != null) {
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskRuntime.addCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public void deleteCandidateGroups(
        @PathVariable String taskId,
        @RequestBody CandidateGroupsPayload candidateGroupsPayload
    ) {
        if (candidateGroupsPayload != null) {
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskRuntime.deleteCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public CollectionModel<EntityModel<CandidateGroup>> getGroupCandidates(@PathVariable String taskId) {
        groupCandidatesRepresentationModelAssembler.setTaskId(taskId);
        return resourcesAssembler.toCollectionModel(
            toCandidateGroupConverter.from(taskRuntime.groupCandidates(taskId)),
            groupCandidatesRepresentationModelAssembler,
            linkTo(
                methodOn(this.getClass()).getGroupCandidates(groupCandidatesRepresentationModelAssembler.getTaskId())
            )
                .withSelfRel()
        );
    }
}
