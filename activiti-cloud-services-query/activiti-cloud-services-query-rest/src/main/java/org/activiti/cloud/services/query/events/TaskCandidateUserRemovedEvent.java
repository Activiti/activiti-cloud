package org.activiti.cloud.services.query.events;

import org.activiti.cloud.services.query.model.TaskCandidateUser;

public class TaskCandidateUserRemovedEvent extends AbstractProcessEngineEvent  {

    private TaskCandidateUser taskCandidateUser;

    public TaskCandidateUserRemovedEvent(){

    }

    public TaskCandidateUserRemovedEvent(Long timestamp,
                                         String eventType,
                                         String executionId,
                                         String processDefinitionId,
                                         String processInstanceId,
                                         String serviceName,
                                         String serviceFullName,
                                         String serviceType,
                                         String serviceVersion,
                                         String appName,
                                         String appVersion,
                                         TaskCandidateUser taskCandidateUser) {
        super(timestamp,
                eventType,
                executionId,
                processDefinitionId,
                processInstanceId,
                serviceName,
                serviceFullName,
                serviceType,
                serviceVersion,
                appName,
                appVersion);
        this.taskCandidateUser = taskCandidateUser;
    }

    public TaskCandidateUser getTaskCandidateUser(){
        return taskCandidateUser;
    }
}
