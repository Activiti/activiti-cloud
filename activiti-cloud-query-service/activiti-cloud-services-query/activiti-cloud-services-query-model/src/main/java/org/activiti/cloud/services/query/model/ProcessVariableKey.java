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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public record ProcessVariableKey(String processDefinitionKey, String variableName) {
    @JsonSetter
    public static ProcessVariableKey fromString(String processVariableKey) {
        String[] parts = processVariableKey.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid process variable key: " + processVariableKey);
        }
        return new ProcessVariableKey(parts[0], parts[1]);
    }

    @JsonGetter
    public static String toString(ProcessVariableKey processVariableKey) {
        return processVariableKey.processDefinitionKey() + "/" + processVariableKey.variableName();
    }
}
