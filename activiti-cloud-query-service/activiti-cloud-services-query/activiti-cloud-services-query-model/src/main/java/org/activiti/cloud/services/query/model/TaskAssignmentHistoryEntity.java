/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import com.introproventures.graphql.jpa.query.annotation.GraphQLIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity(name = "TaskAssignmentHistory")
@Table(
    name = "task_assignment_history",
    indexes = @Index(name = "idx_task_assignment_history_task_time", columnList = "task_id,timestamp,id")
)
@GraphQLIgnore
public class TaskAssignmentHistoryEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    private String actor;

    @Column(name = "previous_assignee")
    private String previousAssignee;

    @Column(name = "new_assignee")
    private String newAssignee;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(nullable = false)
    private Long timestamp;

    @Column(length = 255)
    private String reason;

    public TaskAssignmentHistoryEntity() {}

    public TaskAssignmentHistoryEntity(
        String id,
        String taskId,
        String actor,
        String previousAssignee,
        String newAssignee,
        String action,
        Long timestamp,
        String reason
    ) {
        this.id = id;
        this.taskId = taskId;
        this.actor = actor;
        this.previousAssignee = previousAssignee;
        this.newAssignee = newAssignee;
        this.action = action;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getActor() {
        return actor;
    }

    public String getPreviousAssignee() {
        return previousAssignee;
    }

    public String getNewAssignee() {
        return newAssignee;
    }

    public String getAction() {
        return action;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }
}
