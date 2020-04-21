package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class UserCandidatesRepresentationModelAssembler implements RepresentationModelAssembler<CandidateUser, EntityModel<CandidateUser>>{

    private String taskId;

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public EntityModel<CandidateUser> toModel(CandidateUser candidateUser) {
        return new EntityModel<>(candidateUser);
    }
}
