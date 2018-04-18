package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class TaskCandidateGroupRemovedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateGroupRemovedEvent {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupRemovedEventImpl() {
    }

    public TaskCandidateGroupRemovedEventImpl(Service service,
                                              Application application,
                                              String executionId,
                                              String processDefinitionId,
                                              String processInstanceId,
                                              TaskCandidateGroup taskCandidateGroup) {
        super(service,
              application,
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
