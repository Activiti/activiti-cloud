package org.activiti.cloud.services.api.model.converter;

import org.activiti.cloud.services.api.model.TaskCandidateGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskCandidateGroupConverter implements ModelConverter<org.activiti.engine.task.IdentityLink, TaskCandidateGroup> {

    private final ListConverter listConverter;

    @Autowired
    public TaskCandidateGroupConverter(ListConverter listConverter) {
        this.listConverter = listConverter;
    }

    @Override
    public TaskCandidateGroup from(org.activiti.engine.task.IdentityLink source) {
        TaskCandidateGroup taskCandidateGroup = null;

        if (source != null) {
            taskCandidateGroup = new TaskCandidateGroup(source.getGroupId(),source.getTaskId());
        }
        return taskCandidateGroup;
    }

    @Override
    public List<TaskCandidateGroup> from(List<org.activiti.engine.task.IdentityLink> identityLinks) {
        return listConverter.from(identityLinks,
                this);
    }
}
