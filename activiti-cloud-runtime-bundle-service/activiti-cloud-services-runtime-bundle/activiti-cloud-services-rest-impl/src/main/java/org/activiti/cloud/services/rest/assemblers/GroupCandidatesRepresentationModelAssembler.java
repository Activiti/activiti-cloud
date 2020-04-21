package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class GroupCandidatesRepresentationModelAssembler implements RepresentationModelAssembler<CandidateGroup, EntityModel<CandidateGroup>> {

    private String taskId;

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public EntityModel<CandidateGroup> toModel(CandidateGroup groupCandidates) {
        return new EntityModel<>(groupCandidates);
    }
}
