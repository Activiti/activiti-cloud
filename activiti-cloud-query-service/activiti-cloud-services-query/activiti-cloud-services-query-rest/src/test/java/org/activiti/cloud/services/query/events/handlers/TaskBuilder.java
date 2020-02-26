/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.events.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskBuilder {

    private TaskEntity taskEntity;

    private TaskBuilder() {
        this.taskEntity = mock(TaskEntity.class);
    }

    public static TaskBuilder aTask() {
        return new TaskBuilder();
    }

    public TaskBuilder withId(String taskId) {
        when(taskEntity.getId()).thenReturn(taskId);
        return this;
    }
    
    public TaskBuilder withName(String name) {
        when(taskEntity.getName()).thenReturn(name);
        return this;
    }

    public TaskBuilder withDescription(String description) {
        when(taskEntity.getDescription()).thenReturn(description);
        return this;
    }
    
    public TaskBuilder withPriority(Integer priority) {
        when(taskEntity.getPriority()).thenReturn(priority);
        return this;
    }
    
    public TaskBuilder withDueDate(Date dueDate) {
        when(taskEntity.getDueDate()).thenReturn(dueDate);
        return this;
    }
    
    public TaskBuilder withFormKey(String formKey) {
        when(taskEntity.getFormKey()).thenReturn(formKey);
        return this;
    }
    
    public TaskBuilder withParentTaskId(String parentTaskId) {
        when(taskEntity.getParentTaskId()).thenReturn(parentTaskId);
        return this;
    }
    
    public TaskBuilder withAssignee(String assignee) {
        when(taskEntity.getAssignee()).thenReturn(assignee);
        return this;
    }

    public TaskBuilder withCreatedDate(Date createdDate){
        when(taskEntity.getCreatedDate()).thenReturn(createdDate);
        return this;
    }

    public TaskBuilder withCompletedDate(Date completedDate){
        when(taskEntity.getCompletedDate()).thenReturn(completedDate);
        return this;
    }
    
    public TaskEntity build() {
        return taskEntity;
    }
}
