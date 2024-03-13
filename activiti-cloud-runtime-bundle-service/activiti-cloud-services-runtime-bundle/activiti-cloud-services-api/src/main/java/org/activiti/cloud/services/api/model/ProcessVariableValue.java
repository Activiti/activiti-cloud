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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ProcessVariableValue implements Serializable {

    private static final long serialVersionUID = 1L;
    private String type;
    private Serializable value;

    ProcessVariableValue() {}

    public ProcessVariableValue(String type, Serializable value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Serializable getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProcessVariableValue other = (ProcessVariableValue) obj;
        return Objects.equals(type, other.type) && Objects.equals(value, other.value);
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> result = new LinkedHashMap<>(3);

        result.put("type", type);
        result.put("value", value);

        return result;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"").append(type).append("\",\"value\":").append(value).append("}");
        return builder.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
