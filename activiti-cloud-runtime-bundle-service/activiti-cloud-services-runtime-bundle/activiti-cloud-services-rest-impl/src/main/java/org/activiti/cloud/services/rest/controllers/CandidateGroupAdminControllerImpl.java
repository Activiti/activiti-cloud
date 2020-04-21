package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.services.rest.api.CandidateGroupAdminController;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class CandidateGroupAdminControllerImpl implements CandidateGroupAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final CollectionModelAssembler resourcesAssembler;

    private final GroupCandidatesRepresentationModelAssembler groupCandidatesRepresentationModelAssembler;

    private final ToCandidateGroupConverter toCandidateGroupConverter;

    public CandidateGroupAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                             GroupCandidatesRepresentationModelAssembler groupCandidatesRepresentationModelAssembler,
                                             ToCandidateGroupConverter toCandidateGroupConverter,
                                             CollectionModelAssembler resourcesAssembler) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.groupCandidatesRepresentationModelAssembler = groupCandidatesRepresentationModelAssembler;
        this.toCandidateGroupConverter = toCandidateGroupConverter;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public void addCandidateGroups(@PathVariable String taskId,
                                   @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null) {
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskAdminRuntime.addCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public void deleteCandidateGroups(@PathVariable String taskId,
                                      @RequestBody CandidateGroupsPayload candidateGroupsPayload) {
        if (candidateGroupsPayload!=null) {
            candidateGroupsPayload.setTaskId(taskId);
        }
        taskAdminRuntime.deleteCandidateGroups(candidateGroupsPayload);
    }

    @Override
    public CollectionModel<EntityModel<CandidateGroup>> getGroupCandidates(@PathVariable String taskId) {
        groupCandidatesRepresentationModelAssembler.setTaskId(taskId);
        return resourcesAssembler.toCollectionModel(toCandidateGroupConverter.from(taskAdminRuntime.groupCandidates(taskId)),
                                              groupCandidatesRepresentationModelAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getGroupCandidates(groupCandidatesRepresentationModelAssembler.getTaskId()))
                                                      .withSelfRel());
    }
}
