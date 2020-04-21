package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.services.rest.api.CandidateUserAdminController;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesRepresentationModelAssembler;
import org.activiti.cloud.services.rest.assemblers.CollectionModelAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
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
public class CandidateUserAdminControllerImpl implements CandidateUserAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final UserCandidatesRepresentationModelAssembler userCandidatesRepresentationModelAssembler;

    private final ToCandidateUserConverter toCandidateUserConverter;

    private final CollectionModelAssembler resourcesAssembler;

    public CandidateUserAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                            UserCandidatesRepresentationModelAssembler userCandidatesRepresentationModelAssembler,
                                            ToCandidateUserConverter toCandidateUserConverter,
                                            CollectionModelAssembler resourcesAssembler) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.userCandidatesRepresentationModelAssembler = userCandidatesRepresentationModelAssembler;
        this.toCandidateUserConverter = toCandidateUserConverter;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public void addCandidateUsers(@PathVariable String taskId,
                                  @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null) {
            candidateUsersPayload.setTaskId(taskId);
        }
        taskAdminRuntime.addCandidateUsers(candidateUsersPayload);
    }

    @Override
    public void deleteCandidateUsers(@PathVariable String taskId,
                                     @RequestBody CandidateUsersPayload candidateUsersPayload) {
        if (candidateUsersPayload!=null) {
            candidateUsersPayload.setTaskId(taskId);
        }
        taskAdminRuntime.deleteCandidateUsers(candidateUsersPayload);

    }

    @Override
    public CollectionModel<EntityModel<CandidateUser>> getUserCandidates(@PathVariable String taskId) {
        userCandidatesRepresentationModelAssembler.setTaskId(taskId);
        return resourcesAssembler.toCollectionModel(toCandidateUserConverter.from(taskAdminRuntime.userCandidates(taskId)),
                                              userCandidatesRepresentationModelAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getUserCandidates(userCandidatesRepresentationModelAssembler.getTaskId()))
                                                             .withSelfRel());
    }


}
