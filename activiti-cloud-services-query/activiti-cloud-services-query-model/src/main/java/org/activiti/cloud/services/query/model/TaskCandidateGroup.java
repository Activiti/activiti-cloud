package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name="TaskCandidateGroup")
@Table(name="TASK_CANDIDATE_GROUP", indexes= {
		@Index(name="tcg_groupId_idx", columnList="groupId", unique=false),
		@Index(name="tcg_taskId_idx", columnList="taskId", unique=false)
	}
)
@IdClass(TaskCandidateGroupId.class)
public class TaskCandidateGroup {

    @Id
    private String taskId;

    @Id
    private String groupId;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false, nullable = true
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private TaskEntity task;

    public TaskCandidateGroup() {

    }

    public TaskCandidateGroup(String taskid,
                              String groupId) {
        this.taskId = taskid;
        this.groupId = groupId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public TaskEntity getTask() {
        return this.task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, taskId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskCandidateGroup other = (TaskCandidateGroup) obj;
        return Objects.equals(groupId, other.groupId) && Objects.equals(taskId, other.taskId);
    }
}
