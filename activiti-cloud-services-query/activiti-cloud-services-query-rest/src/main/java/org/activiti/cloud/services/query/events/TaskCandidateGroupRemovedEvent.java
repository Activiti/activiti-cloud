package org.activiti.cloud.services.query.events;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.query.model.TaskCandidateGroup;

public class TaskCandidateGroupRemovedEvent extends AbstractProcessEngineEvent  {

    private TaskCandidateGroup taskCandidateGroup;

    public TaskCandidateGroupRemovedEvent(){

    }

    public TaskCandidateGroupRemovedEvent(Long timestamp,
                                          String eventType,
                                          String executionId,
                                          String processDefinitionId,
                                          String processInstanceId,
                                          Service service,
                                          Application application,
                                          TaskCandidateGroup taskCandidateGroup) {
        super(timestamp,
                eventType,
                executionId,
                processDefinitionId,
                processInstanceId,
                service,
                application);
        this.taskCandidateGroup = taskCandidateGroup;
    }

    public TaskCandidateGroup getTaskCandidateGroup(){
        return taskCandidateGroup;
    }
}
