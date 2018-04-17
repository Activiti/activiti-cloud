package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class TaskCandidateGroupRemovedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateGroupRemovedEvent {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupRemovedEventImpl() {
    }

    public TaskCandidateGroupRemovedEventImpl(String fullyQualifiedServiceName,
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
        return "TaskCandidateGroupRemovedEvent";
    }
}
