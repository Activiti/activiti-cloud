package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.introproventures.graphql.jpa.query.annotation.GraphQLDescription;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@GraphQLDescription("Task Candidate User Entity Model")

@Entity
@IdClass(TaskCandidateUserId.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskCandidateUser {

    @Id
    private String taskId;

    @Id
    private String userId;


    @JsonIgnore
    @ManyToOne(optional=true)
    @JoinColumn(name="taskId", referencedColumnName="id", insertable=false, updatable=false, nullable=true
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name="none"))
    private Task task;

    @JsonCreator
    public TaskCandidateUser(@JsonProperty("taskId") String taskid,
                             @JsonProperty("userId") String userId) {
        this.taskId = taskid;
        this.userId = userId;
    }

    public TaskCandidateUser(){

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

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
