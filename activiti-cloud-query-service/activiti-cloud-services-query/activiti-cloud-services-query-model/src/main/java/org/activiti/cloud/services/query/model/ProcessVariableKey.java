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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public final class ProcessVariableKey {

    private final String processDefinitionKey;
    private final String variableName;

    public ProcessVariableKey(String processDefinitionKey, String variableName) {
        this.processDefinitionKey = processDefinitionKey;
        this.variableName = variableName;
    }

    @JsonCreator
    public ProcessVariableKey(String key) {
        String[] parts = key.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Cannot deserialize key " +
                key +
                " into ProcessVariableKey. Key pattern must be {processDefinitionKey}/{variableName}"
            );
        }
        this.processDefinitionKey = parts[0];
        this.variableName = parts[1];
    }

    @JsonValue
    public String key() {
        return processDefinitionKey + "/" + variableName;
    }

    public String processDefinitionKey() {
        return processDefinitionKey;
    }

    public String variableName() {
        return variableName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProcessVariableKey) obj;
        return (
            Objects.equals(this.processDefinitionKey, that.processDefinitionKey) &&
            Objects.equals(this.variableName, that.variableName)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(processDefinitionKey, variableName);
    }

    @Override
    public String toString() {
        return (
            "ProcessVariableKey[" +
            "processDefinitionKey=" +
            processDefinitionKey +
            ", " +
            "variableName=" +
            variableName +
            ']'
        );
    }
}
