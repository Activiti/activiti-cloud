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

package org.activiti.cloud.qa.model.commands;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTaskCmd {

    private final String id;
    private final String commandType;
    private String name;
    private String description;
    private String category;

    public CreateTaskCmd(@JsonProperty("name") String name,
                         @JsonProperty("description") String description,
                         @JsonProperty("category") String category,
                         @JsonProperty("commandType") String commandType) {

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.category = category;
        this.commandType = commandType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getCommandType() {
        return commandType;
    }
}
