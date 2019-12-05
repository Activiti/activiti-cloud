package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

public class GroupCandidatesResourceAssembler implements ResourceAssembler<CandidateGroup, Resource<CandidateGroup>> {

    private String taskId;

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public Resource<CandidateGroup> toResource(CandidateGroup groupCandidates) {
        return new Resource<>(groupCandidates);
    }
}
