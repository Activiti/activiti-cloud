package org.activiti.cloud.services.query.model;

import java.io.Serializable;

public class TaskCandidateGroupId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String groupId;

    public TaskCandidateGroupId() {

    }

    public TaskCandidateGroupId(String taskId, String groupId) {
        this.taskId = taskId;
        this.groupId = groupId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + groupId.hashCode();
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
        TaskCandidateGroupId other = (TaskCandidateGroupId) obj;
        if (taskId == null) {
            if (other.taskId != null)
                return false;
        } else if (!taskId.equals(other.taskId))
            return false;
        if (groupId != other.groupId)
            return false;
        return true;
    }

}
