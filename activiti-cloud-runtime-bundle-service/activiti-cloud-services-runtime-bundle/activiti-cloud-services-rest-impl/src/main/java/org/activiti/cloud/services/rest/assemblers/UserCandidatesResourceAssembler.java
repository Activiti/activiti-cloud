package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

public class UserCandidatesResourceAssembler implements ResourceAssembler<CandidateUser, Resource<CandidateUser>>{

    private String taskId;

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public Resource<CandidateUser> toResource(CandidateUser candidateUser) {
        return new Resource<>(candidateUser);
    }
}
