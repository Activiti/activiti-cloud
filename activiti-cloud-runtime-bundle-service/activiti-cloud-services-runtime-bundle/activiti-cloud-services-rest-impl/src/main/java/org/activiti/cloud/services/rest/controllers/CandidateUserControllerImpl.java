package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.services.rest.api.CandidateUserController;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateUserConverter;
import org.activiti.cloud.services.rest.assemblers.UserCandidatesRepresentationModelAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class CandidateUserControllerImpl implements CandidateUserController {

    private final TaskRuntime taskRuntime;

    private final UserCandidatesRepresentationModelAssembler userCandidatesRepresentationModelAssembler;

    private final CollectionModelAssembler resourcesAssembler;

    private final ToCandidateUserConverter toCandidateUserConverter;

    public CandidateUserControllerImpl(TaskRuntime taskRuntime,
                                       UserCandidatesRepresentationModelAssembler userCandidatesRepresentationModelAssembler,
                                       CollectionModelAssembler resourcesAssembler,
                                       ToCandidateUserConverter toCandidateUserConverter) {
        this.taskRuntime = taskRuntime;
        this.userCandidatesRepresentationModelAssembler = userCandidatesRepresentationModelAssembler;
        this.resourcesAssembler = resourcesAssembler;
        this.toCandidateUserConverter = toCandidateUserConverter;
    }

    @Override
    public void addCandidateUsers(@PathVariable String taskId,
                                  @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null) {
            candidateUsersPayload.setTaskId(taskId);
        }
        taskRuntime.addCandidateUsers(candidateUsersPayload);
    }

    @Override
    public void deleteCandidateUsers(@PathVariable String taskId,
                                     @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null) {
            candidateUsersPayload.setTaskId(taskId);
        }
        taskRuntime.deleteCandidateUsers(candidateUsersPayload);

    }

    @Override
    public CollectionModel<EntityModel<CandidateUser>> getUserCandidates(@PathVariable String taskId) {
        userCandidatesRepresentationModelAssembler.setTaskId(taskId);
        return resourcesAssembler.toCollectionModel(toCandidateUserConverter.from(taskRuntime.userCandidates(taskId)),
                                              userCandidatesRepresentationModelAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getUserCandidates(userCandidatesRepresentationModelAssembler.getTaskId()))
                                                      .withSelfRel());
    }

}
