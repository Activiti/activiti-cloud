/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.*;
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
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "TaskCandidateUser")
@IdClass(TaskCandidateUserId.class)
@Table(
    name = "TASK_CANDIDATE_USER",
    indexes = {
        @Index(name = "tcu_userId_idx", columnList = "userId", unique = false),
        @Index(name = "tcu_taskId_idx", columnList = "taskId", unique = false),
    }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamicInsert
@DynamicUpdate
public class TaskCandidateUserEntity {

    @Id
    private String taskId;

    @Id
    private String userId;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "taskId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        nullable = true,
        foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private TaskEntity task;

    @JsonCreator
    public TaskCandidateUserEntity(@JsonProperty("taskId") String taskid, @JsonProperty("userId") String userId) {
        this.taskId = taskid;
        this.userId = userId;
    }

    public TaskCandidateUserEntity() {}

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
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TaskCandidateUserEntity other = (TaskCandidateUserEntity) obj;
        return (
            this.userId != null &&
            this.taskId != null &&
            Objects.equals(taskId, other.taskId) &&
            Objects.equals(userId, other.userId)
        );
    }
}
