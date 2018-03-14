package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCandidateGroup
{
    public String groupId;
    public String taskId;

    public TaskCandidateGroup(){

    }

    public TaskCandidateGroup(String groupId, String taskId){
        this.groupId = groupId;
        this.taskId = taskId;
    }


    public String getGroupId() {
        return groupId;
    }

    public String getTaskId() {
        return taskId;
    }

}
