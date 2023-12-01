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

public class ProcessDefinitionVariable extends JsonDeserializer<Set<ProcessDefinitionVariable>> {

    @JsonProperty("variableName")
    private String variableName;

    @JsonProperty("variableType")
    private String variableType;

    public ProcessDefinitionVariable() {}

    @JsonCreator
    public ProcessDefinitionVariable(String variableName, String variableType) {
        this.variableName = variableName;
        this.variableType = variableType;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableType() {
        return variableType;
    }

    @Override
    public Set<ProcessDefinitionVariable> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        Set<ProcessDefinitionVariable> variables = new HashSet<ProcessDefinitionVariable>();
        ObjectCodec oc = jp.getCodec();
        JsonNode nodes = oc.readTree(jp);

        for (int i = 0; i < nodes.size(); i++) {
            ProcessDefinitionVariable variable = new ProcessDefinitionVariable(
                nodes.get(i).get("variableName").asText(),
                nodes.get(i).get("variableType").asText()
            );
            variables.add(variable);
        }

        return variables;
    }
}
