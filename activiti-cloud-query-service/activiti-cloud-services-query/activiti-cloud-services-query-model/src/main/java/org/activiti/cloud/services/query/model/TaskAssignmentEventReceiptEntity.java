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

@Entity(name = "TaskAssignmentEventReceipt")
@Table(
    name = "task_assignment_event_receipt",
    indexes = @Index(name = "idx_task_assignment_event_task", columnList = "task_id")
)
@GraphQLIgnore
public class TaskAssignmentEventReceiptEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    public TaskAssignmentEventReceiptEntity() {}

    public TaskAssignmentEventReceiptEntity(String id, String taskId) {
        this.id = id;
        this.taskId = taskId;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }
}
