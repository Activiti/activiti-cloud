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

package org.activiti.cloud.services.api.commands;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTaskCmd implements Command {

    private final String id;
    private String name;
    private String description;
    private Date dueDate;
    private Integer priority;
    private String assignee;

    public CreateTaskCmd() {
        this.id = UUID.randomUUID().toString();
    }

    @JsonCreator
    public CreateTaskCmd(@JsonProperty("name") String name,
                         @JsonProperty("description") String description,
                         @JsonProperty("dueDate") Date dueDate,
                         @JsonProperty("priority") Integer priority,
                         @JsonProperty("assignee") String assignee) {

        this();
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.assignee = assignee;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }
}
