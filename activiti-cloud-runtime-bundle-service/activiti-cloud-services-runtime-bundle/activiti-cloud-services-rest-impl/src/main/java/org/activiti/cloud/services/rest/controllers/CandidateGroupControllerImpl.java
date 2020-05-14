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

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.services.rest.api.CandidateGroupController;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ResourcesAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class CandidateGroupControllerImpl implements CandidateGroupController {

    private final TaskRuntime taskRuntime;

    private final ResourcesAssembler resourcesAssembler;

    private final GroupCandidatesResourceAssembler groupCandidatesResourceAssembler;

    private final ToCandidateGroupConverter toCandidateGroupConverter;

    public CandidateGroupControllerImpl(TaskRuntime taskRuntime,
                                        ResourcesAssembler resourcesAssembler,
                                        GroupCandidatesResourceAssembler groupCandidatesResourceAssembler,
                                        ToCandidateGroupConverter toCandidateGroupConverter) {
        this.taskRuntime = taskRuntime;
        this.resourcesAssembler = resourcesAssembler;
        this.groupCandidatesResourceAssembler = groupCandidatesResourceAssembler;
        this.toCandidateGroupConverter = toCandidateGroupConverter;
    }

    @Override
    public void addCandidateGroups(@PathVariable String taskId,
                                   @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null) {
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskRuntime.addCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public void deleteCandidateGroups(@PathVariable String taskId,
                                      @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null){
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskRuntime.deleteCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public Resources<Resource<CandidateGroup>> getGroupCandidates(@PathVariable String taskId) {
        groupCandidatesResourceAssembler.setTaskId(taskId);
        return resourcesAssembler.toResources(toCandidateGroupConverter.from(taskRuntime.groupCandidates(taskId)),
                                              groupCandidatesResourceAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getGroupCandidates(groupCandidatesResourceAssembler.getTaskId()))
                                                      .withSelfRel());
    }

}
