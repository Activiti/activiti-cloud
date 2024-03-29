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
package org.activiti.cloud.acc.shared.steps;

import java.util.HashMap;
import java.util.Map;
import net.thucydides.core.annotations.Step;

public class VariableBufferSteps {

    private Map<String, Object> variables = new HashMap<>();

    @Step
    public void addVariable(String name, Object value) {
        variables.put(name, value);
    }

    @Step
    public Map<String, Object> availableVariables() {
        return variables;
    }
}
