package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class TaskCandidateGroupAddedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateGroupAddedEvent {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupAddedEventImpl() {
    }

    public TaskCandidateGroupAddedEventImpl(String fullyQualifiedServiceName,
                                            String executionId,
                                            String processDefinitionId,
                                            String processInstanceId,
                                            TaskCandidateGroup taskCandidateGroup) {
        super(fullyQualifiedServiceName,
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
