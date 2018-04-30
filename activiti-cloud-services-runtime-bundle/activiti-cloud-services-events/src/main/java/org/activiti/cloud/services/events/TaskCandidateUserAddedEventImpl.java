package org.activiti.cloud.services.events;



import org.activiti.cloud.services.api.model.TaskCandidateUser;

public class TaskCandidateUserAddedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateUserAddedEvent {

    private TaskCandidateUser taskCandidateUser;

    public TaskCandidateUserAddedEventImpl() {
    }

    public TaskCandidateUserAddedEventImpl(String appName, String appVersion, String serviceName, String serviceFullName, String serviceType, String serviceVersion,

                                           String executionId,
                                           String processDefinitionId,
                                           String processInstanceId,
                                           TaskCandidateUser taskCandidateUser) {
        super(appName,appVersion,serviceName,serviceFullName,serviceType,serviceVersion,

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
        return "TaskCandidateUserAddedEvent";
    }

}
