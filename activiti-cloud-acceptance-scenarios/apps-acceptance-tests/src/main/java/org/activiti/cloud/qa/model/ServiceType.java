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
package org.activiti.cloud.qa.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceType {
    AUDIT("audit"),
    QUERY("query"),
    CONNECTOR("connector"),
    RUNTIME_BUNDLE("runtime-bundle");

    private final String value;

    ServiceType(final String type) {
        value = type;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    public boolean equals(ServiceType serviceType){
        return serviceType.toString().equals(value);
    }
}
