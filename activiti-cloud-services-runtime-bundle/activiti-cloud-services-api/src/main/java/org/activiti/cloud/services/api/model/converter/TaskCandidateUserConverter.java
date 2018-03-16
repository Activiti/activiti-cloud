package org.activiti.cloud.services.api.model.converter;

import org.activiti.cloud.services.api.model.TaskCandidateUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskCandidateUserConverter implements ModelConverter<org.activiti.engine.task.IdentityLink, TaskCandidateUser> {

    private final ListConverter listConverter;

    @Autowired
    public TaskCandidateUserConverter(ListConverter listConverter) {
        this.listConverter = listConverter;
    }

    @Override
    public TaskCandidateUser from(org.activiti.engine.task.IdentityLink source) {
        TaskCandidateUser taskCandidateUser = null;

        if (source != null) {
            taskCandidateUser = new TaskCandidateUser(source.getUserId(),source.getTaskId());
        }
        return taskCandidateUser;
    }

    @Override
    public List<TaskCandidateUser> from(List<org.activiti.engine.task.IdentityLink> identityLinks) {
        return listConverter.from(identityLinks,
                this);
    }
}
