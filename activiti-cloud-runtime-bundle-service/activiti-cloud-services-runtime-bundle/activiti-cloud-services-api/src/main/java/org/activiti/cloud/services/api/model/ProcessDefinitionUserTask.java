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
package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectCodec;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public class ProcessDefinitionUserTask extends ValueDeserializer<Set<ProcessDefinitionUserTask>> {

    @JsonProperty("taskName")
    private String taskName;

    @JsonProperty("taskDocumentation")
    private String taskDocumentation;

    public ProcessDefinitionUserTask() {}

    @JsonCreator
    public ProcessDefinitionUserTask(String name, String documentation) {
        taskName = name;
        taskDocumentation = documentation;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDocumentation() {
        return taskDocumentation;
    }

    @Override
    public Set<ProcessDefinitionUserTask> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JacksonException {
        Set<ProcessDefinitionUserTask> tasks = new HashSet<ProcessDefinitionUserTask>();
        ObjectCodec oc = jp.objectReadContext();
        JsonNode nodes = oc.readTree(jp);

        for (int i = 0; i < nodes.size(); i++) {
            ProcessDefinitionUserTask task = new ProcessDefinitionUserTask(
                nodes.get(i).get("taskName").asString(),
                nodes.get(i).get("taskDocumentation").asString()
            );
            tasks.add(task);
        }

        return tasks;
    }
}
