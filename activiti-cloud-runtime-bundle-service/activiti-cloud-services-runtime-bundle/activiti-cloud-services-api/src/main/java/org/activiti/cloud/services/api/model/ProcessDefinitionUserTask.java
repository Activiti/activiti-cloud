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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ProcessDefinitionUserTask extends JsonDeserializer<Set<ProcessDefinitionUserTask>> {

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
        throws IOException, JsonProcessingException {
        Set<ProcessDefinitionUserTask> tasks = new HashSet<ProcessDefinitionUserTask>();
        ObjectCodec oc = jp.getCodec();
        JsonNode nodes = oc.readTree(jp);

        for (int i = 0; i < nodes.size(); i++) {
            ProcessDefinitionUserTask task = new ProcessDefinitionUserTask(
                nodes.get(i).get("taskName").asText(),
                nodes.get(i).get("taskDocumentation").asText()
            );
            tasks.add(task);
        }

        return tasks;
    }
}
