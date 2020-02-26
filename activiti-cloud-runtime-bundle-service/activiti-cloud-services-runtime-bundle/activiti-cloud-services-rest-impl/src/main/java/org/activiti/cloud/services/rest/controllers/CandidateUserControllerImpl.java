package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.services.rest.api.CandidateUserController;
import org.activiti.cloud.services.rest.assemblers.ResourcesAssembler;
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
public class CandidateUserControllerImpl implements CandidateUserController {

    private final TaskRuntime taskRuntime;

    private final UserCandidatesResourceAssembler userCandidatesResourceAssembler;

    private final ResourcesAssembler resourcesAssembler;

    private final ToCandidateUserConverter toCandidateUserConverter;

    public CandidateUserControllerImpl(TaskRuntime taskRuntime,
                                       UserCandidatesResourceAssembler userCandidatesResourceAssembler,
                                       ResourcesAssembler resourcesAssembler,
                                       ToCandidateUserConverter toCandidateUserConverter) {
        this.taskRuntime = taskRuntime;
        this.userCandidatesResourceAssembler = userCandidatesResourceAssembler;
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
    public Resources<Resource<CandidateUser>> getUserCandidates(@PathVariable String taskId) {
        userCandidatesResourceAssembler.setTaskId(taskId);
        return resourcesAssembler.toResources(toCandidateUserConverter.from(taskRuntime.userCandidates(taskId)),
                                              userCandidatesResourceAssembler,
                                              linkTo(methodOn(this.getClass())
                                                             .getUserCandidates(userCandidatesResourceAssembler.getTaskId()))
                                                      .withSelfRel());
    }

}
