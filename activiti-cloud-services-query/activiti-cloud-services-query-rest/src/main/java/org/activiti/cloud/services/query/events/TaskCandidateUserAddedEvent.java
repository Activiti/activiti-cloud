package org.activiti.cloud.services.query.events;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
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
                                       Service service,
                                       Application application,
                                       TaskCandidateUser taskCandidateUser) {
        super(timestamp,
                eventType,
                executionId,
                processDefinitionId,
                processInstanceId,
                service,
                application);
        this.taskCandidateUser = taskCandidateUser;
    }

    public TaskCandidateUser getTaskCandidateUser(){
        return taskCandidateUser;
    }
}
