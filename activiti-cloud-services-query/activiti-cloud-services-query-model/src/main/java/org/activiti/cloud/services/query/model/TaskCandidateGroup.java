package org.activiti.cloud.services.query.model;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name="TaskCandidateGroup")
@IdClass(TaskCandidateGroupId.class)
public class TaskCandidateGroup {

    @Id
    private String taskId;

    @Id
    private String groupId;

    @ManyToOne(optional = true)
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
}
