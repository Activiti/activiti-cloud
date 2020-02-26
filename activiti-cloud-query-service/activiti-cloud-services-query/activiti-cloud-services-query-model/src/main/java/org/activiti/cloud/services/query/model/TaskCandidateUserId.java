package org.activiti.cloud.services.query.model;

import java.io.Serializable;

public class TaskCandidateUserId implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String taskId;
    private String userId;

    public TaskCandidateUserId() {

    }

    public TaskCandidateUserId(String taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + userId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskCandidateUserId other = (TaskCandidateUserId) obj;
        if (taskId == null) {
            if (other.taskId != null)
                return false;
        } else if (!taskId.equals(other.taskId))
            return false;
        if (userId != other.userId)
            return false;
        return true;
    }

}
