package org.activiti.cloud.services.query.events;

import org.activiti.cloud.services.query.model.TaskCandidateUser;

public class TaskCandidateUserAddedEvent  extends AbstractProcessEngineEvent  {

    private TaskCandidateUser taskCandidateUser;

    public TaskCandidateUserAddedEvent(){

    }

    public TaskCandidateUserAddedEvent(Long timestamp,
                                       String eventType,
                                       String executionId,
                                       String processDefinitionId,
                                       String processInstanceId,
                                       String applicationName,
                                       TaskCandidateUser taskCandidateUser) {
        super(timestamp,
                eventType,
                executionId,
                processDefinitionId,
                processInstanceId,
                applicationName);
        this.taskCandidateUser = taskCandidateUser;
    }

    public TaskCandidateUser getTaskCandidateUser(){
        return taskCandidateUser;
    }
}
