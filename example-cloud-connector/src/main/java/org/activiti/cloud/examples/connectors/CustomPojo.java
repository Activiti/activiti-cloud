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
package org.activiti.cloud.examples.connectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomPojo {

    @JsonProperty("test-json-variable-element1")
    private String testJsonVariableElement1;

    public String getTestJsonVariableElement1() {
        return testJsonVariableElement1;
    }

    public void setTestJsonVariableElement1(String testJsonVariableElement1) {
        this.testJsonVariableElement1 = testJsonVariableElement1;
    }
}
