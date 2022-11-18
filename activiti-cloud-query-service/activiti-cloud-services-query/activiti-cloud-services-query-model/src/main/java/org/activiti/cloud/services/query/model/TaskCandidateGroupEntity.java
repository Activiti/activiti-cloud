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

import java.util.Objects;
import javax.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "TaskCandidateGroup")
@Table(
    name = "TASK_CANDIDATE_GROUP",
    indexes = {
        @Index(name = "tcg_groupId_idx", columnList = "groupId", unique = false),
        @Index(name = "tcg_taskId_idx", columnList = "taskId", unique = false)
    }
)
@IdClass(TaskCandidateGroupId.class)
@DynamicInsert
@DynamicUpdate
public class TaskCandidateGroupEntity {

    @Id
    private String taskId;

    @Id
    private String groupId;

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

    public TaskCandidateGroupEntity() {}

    public TaskCandidateGroupEntity(String taskid, String groupId) {
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
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TaskCandidateGroupEntity other = (TaskCandidateGroupEntity) obj;
        return (
            this.groupId != null &&
            this.taskId != null &&
            Objects.equals(groupId, other.groupId) &&
            Objects.equals(taskId, other.taskId)
        );
    }
}
