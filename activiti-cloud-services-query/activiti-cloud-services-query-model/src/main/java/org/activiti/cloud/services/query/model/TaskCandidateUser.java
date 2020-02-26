package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Objects;

@Entity(name="TaskCandidateUser")
@IdClass(TaskCandidateUserId.class)
@Table(name="TASK_CANDIDATE_USER", indexes= {
		@Index(name="tcu_userId_idx", columnList="userId", unique=false),
		@Index(name="tcu_taskId_idx", columnList="taskId", unique=false)
	}
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskCandidateUser {

    @Id
    private String taskId;

    @Id
    private String userId;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false, nullable = true
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private TaskEntity task;

    @JsonCreator
    public TaskCandidateUser(@JsonProperty("taskId") String taskid,
                             @JsonProperty("userId") String userId) {
        this.taskId = taskid;
        this.userId = userId;
    }

    public TaskCandidateUser() {

    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TaskEntity getTask() {
        return this.task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskCandidateUser other = (TaskCandidateUser) obj;
        return Objects.equals(taskId, other.taskId) && Objects.equals(userId, other.userId);
    }
}
