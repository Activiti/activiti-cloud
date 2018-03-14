package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCandidateUser
{
    public String userId;
    public String taskId;

    public TaskCandidateUser(){

    }

    public TaskCandidateUser(String userId, String taskId){
        this.userId = userId;
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTaskId() {
        return taskId;
    }

}
