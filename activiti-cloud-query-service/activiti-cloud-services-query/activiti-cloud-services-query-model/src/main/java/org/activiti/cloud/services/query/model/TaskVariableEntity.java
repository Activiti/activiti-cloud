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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.Objects;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity(name = "TaskVariable")
@Table(
    name = "TASK_VARIABLE",
    indexes = {
        @Index(name = "task_var_processInstanceId_idx", columnList = "processInstanceId", unique = false),
        @Index(name = "task_var_taskId_idx", columnList = "taskId", unique = false),
        @Index(name = "task_var_name_idx", columnList = "name", unique = false),
        @Index(name = "task_var_executionId_idx", columnList = "executionId", unique = false)
    }
)
@DynamicInsert
@DynamicUpdate
public class TaskVariableEntity extends AbstractVariableEntity {

    @Id
    @GeneratedValue(generator = "task_variable_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "task_variable_sequence", sequenceName = "task_variable_sequence", allocationSize = 50)
    private Long id;

    private String taskId;

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

    public TaskVariableEntity() {}

    public TaskVariableEntity(
        Long id,
        String type,
        String name,
        String processInstanceId,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        String taskId,
        Date createTime,
        Date lastUpdatedTime,
        String executionId
    ) {
        super(
            type,
            name,
            processInstanceId,
            serviceName,
            serviceFullName,
            serviceVersion,
            appName,
            appVersion,
            createTime,
            lastUpdatedTime,
            executionId
        );
        this.id = id;
        this.taskId = taskId;
    }

    @Override
    public Long getId() {
        return id;
    }

    public TaskEntity getTask() {
        return this.task;
    }

    public void setTask(TaskEntity taskEntity) {
        this.task = taskEntity;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public boolean isTaskVariable() {
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        TaskVariableEntity other = (TaskVariableEntity) obj;
        return this.getId() != null && Objects.equals(this.getId(), other.getId());
    }
}
