package org.activiti.cloud.services.events;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.api.model.TaskCandidateGroup;

public class TaskCandidateGroupAddedEventImpl extends AbstractProcessEngineEvent implements TaskCandidateGroupAddedEvent {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupAddedEventImpl() {
    }

    public TaskCandidateGroupAddedEventImpl(Service service,
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
        return "TaskCandidateGroupAddedEvent";
    }

}
