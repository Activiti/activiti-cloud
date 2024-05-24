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
package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessDefinitionMeta {

    private String id;
    private String name;
    private String description;
    private int version;
    private Set<String> users;
    private Set<String> groups;

    @JsonDeserialize(using = ProcessDefinitionVariable.class)
    @Schema(description = "a key value map used to give a value to the variables used in a process.")
    private Set<ProcessDefinitionVariable> variables;

    @JsonDeserialize(using = ProcessDefinitionUserTask.class)
    private Set<ProcessDefinitionUserTask> userTasks;

    @JsonDeserialize(using = ProcessDefinitionServiceTask.class)
    private Set<ProcessDefinitionServiceTask> serviceTasks;

    public ProcessDefinitionMeta() {}

    public ProcessDefinitionMeta(
        String id,
        String name,
        String description,
        int version,
        Set<String> users,
        Set<String> groups,
        Set<ProcessDefinitionVariable> variables,
        Set<ProcessDefinitionUserTask> userTasks,
        Set<ProcessDefinitionServiceTask> serviceTasks
    ) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.users = users;
        this.groups = groups;
        this.variables = variables;
        this.userTasks = userTasks;
        this.serviceTasks = serviceTasks;
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

    public int getVersion() {
        return version;
    }

    public Set<String> getUsers() {
        return users;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public Set<ProcessDefinitionVariable> getVariables() {
        return variables;
    }

    public Set<ProcessDefinitionUserTask> getUserTasks() {
        return userTasks;
    }

    public Set<ProcessDefinitionServiceTask> getServiceTasks() {
        return serviceTasks;
    }
}
