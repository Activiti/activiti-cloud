package org.activiti.cloud.services.events;



import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class TaskCandidateGroupAddedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateGroupAddedEvent {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupAddedEventImpl() {
    }

    public TaskCandidateGroupAddedEventImpl(String appName, String appVersion, String serviceName, String serviceFullName, String serviceType, String serviceVersion,

                                            String executionId,
                                            String processDefinitionId,
                                            String processInstanceId,
                                            TaskCandidateGroup taskCandidateGroup) {
        super(appName,appVersion,serviceName,serviceFullName,serviceType,serviceVersion,

                executionId,
                processDefinitionId,
                processInstanceId);
        this.taskCandidateGroup = taskCandidateGroup;
    }

    public TaskCandidateGroup getTaskCandidateGroup() {
        return taskCandidateGroup;
    }

    @Override
    public String getEventType() {
        return "TaskCandidateGroupAddedEvent";
    }

}
