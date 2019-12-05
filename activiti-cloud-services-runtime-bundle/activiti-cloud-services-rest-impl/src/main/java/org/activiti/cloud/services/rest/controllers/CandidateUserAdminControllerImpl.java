package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.services.rest.api.CandidateUserAdminController;
import org.activiti.cloud.services.rest.assemblers.GroupCandidatesResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ResourcesAssembler;
import org.activiti.cloud.services.rest.assemblers.ToCandidateGroupConverter;
import org.activiti.cloud.services.rest.assemblers.ToCandidateUserConverter;
import org.activiti.cloud.services.rest.assemblers.UserCandidatesResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class CandidateUserAdminControllerImpl implements CandidateUserAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final UserCandidatesResourceAssembler userCandidatesResourceAssembler;

    private final ToCandidateUserConverter toCandidateUserConverter;

    private final ResourcesAssembler resourcesAssembler;

    public CandidateUserAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                            UserCandidatesResourceAssembler userCandidatesResourceAssembler,
                                            ToCandidateUserConverter toCandidateUserConverter,
                                            ResourcesAssembler resourcesAssembler) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.userCandidatesResourceAssembler = userCandidatesResourceAssembler;
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
    public Resources<Resource<CandidateUser>> getUserCandidates(@PathVariable String taskId) {
        userCandidatesResourceAssembler.setTaskId(taskId);
        return resourcesAssembler.toResources(toCandidateUserConverter.from(taskAdminRuntime.userCandidates(taskId)),
                                              userCandidatesResourceAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getUserCandidates(userCandidatesResourceAssembler.getTaskId()))
                                                             .withSelfRel());
    }


}
