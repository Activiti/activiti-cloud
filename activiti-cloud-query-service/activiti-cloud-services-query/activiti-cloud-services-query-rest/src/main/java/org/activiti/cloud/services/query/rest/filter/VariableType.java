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
package org.activiti.cloud.services.query.rest.filter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VariableType {
    STRING,
    INTEGER,
    BIGDECIMAL,
    BOOLEAN,
    DATE,
    DATETIME;

    @JsonValue
    public String getValue() {
        return name().toLowerCase();
    }

    public static VariableType fromString(String name) {
        try {
            return VariableType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Cannot determine variable type from '%s'", name));
        }
    }
}
