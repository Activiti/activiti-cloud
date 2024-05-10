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

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.Immutable;

@Entity
@Table(
    name = "task_process_variable",
    indexes = { @Index(name = "idx_task_process_var", columnList = "task_id, process_variable_id", unique = true) }
)
@Immutable
public class TaskProcessVariableEntity {

    @EmbeddedId
    private TaskProcessVariableId id;

    @ManyToOne
    @MapsId("taskId")
    private TaskEntity task;

    @ManyToOne
    @MapsId("processVariableId")
    @Filter(name = "variablesFilter")
    private ProcessVariableEntity processVariable;

    public TaskProcessVariableId getId() {
        return id;
    }

    public void setId(TaskProcessVariableId id) {
        this.id = id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public ProcessVariableEntity getProcessVariable() {
        return processVariable;
    }

    public void setProcessVariable(ProcessVariableEntity processVariable) {
        this.processVariable = processVariable;
    }

    @Embeddable
    public static class TaskProcessVariableId implements Serializable {

        @JoinColumn(name = "task_id", referencedColumnName = "id")
        private String taskId;

        @JoinColumn(name = "process_variable_id", referencedColumnName = "id")
        private Long processVariableId;

        public TaskProcessVariableId() {}

        public TaskProcessVariableId(String taskId, Long processVariableId) {
            this.taskId = taskId;
            this.processVariableId = processVariableId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public Long getProcessVariableId() {
            return processVariableId;
        }

        public void setProcessVariableId(Long processVariableId) {
            this.processVariableId = processVariableId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TaskProcessVariableId that = (TaskProcessVariableId) o;
            return Objects.equals(taskId, that.taskId) && Objects.equals(processVariableId, that.processVariableId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, processVariableId);
        }
    }
}
