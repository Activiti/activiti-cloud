package org.activiti.cloud.services.query.events;

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
                                          String applicationName,
                                          TaskCandidateGroup taskCandidateGroup) {
        super(timestamp,
                eventType,
                executionId,
                processDefinitionId,
                processInstanceId,
                applicationName);
        this.taskCandidateGroup = taskCandidateGroup;
    }

    public TaskCandidateGroup getTaskCandidateGroup(){
        return taskCandidateGroup;
    }
}
