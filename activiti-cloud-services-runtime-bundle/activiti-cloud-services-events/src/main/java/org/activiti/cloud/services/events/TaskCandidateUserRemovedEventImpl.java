package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.model.TaskCandidateUser;

public class TaskCandidateUserRemovedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateUserRemovedEvent {

    private TaskCandidateUser taskCandidateUser;

    public TaskCandidateUserRemovedEventImpl() {
    }

    public TaskCandidateUserRemovedEventImpl(String applicationName,
                                             String executionId,
                                             String processDefinitionId,
                                             String processInstanceId,
                                             TaskCandidateUser taskCandidateUser) {
        super(applicationName,
              executionId,
              processDefinitionId,
              processInstanceId);
        this.taskCandidateUser = taskCandidateUser;
    }

    public TaskCandidateUser getTaskCandidateUser() {
        return taskCandidateUser;
    }

    @Override
    public String getEventType() {
        return "TaskCandidateUserRemovedEvent";
    }
}
