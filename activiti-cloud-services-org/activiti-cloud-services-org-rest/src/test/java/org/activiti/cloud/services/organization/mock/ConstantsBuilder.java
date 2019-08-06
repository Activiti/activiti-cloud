/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.organization.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.organization.api.process.Constant;

public class ConstantsBuilder {

    private Map<String, Constant> constants = new HashMap<>();

    private String taskName;
    
    public static ConstantsBuilder constantsFor(String taskName) {
        return new ConstantsBuilder(taskName);
    }

    public ConstantsBuilder(String taskName) {
        this.taskName = taskName;
    }

    public ConstantsBuilder add(String name, Object value) {
        Constant constant = new Constant();
        constant.setValue(value);
        constants.put(name, constant);
        return this;
    }

    public Map<String,  Map<String, Constant>> build() {
        return Collections.singletonMap(taskName, constants);
    }

}
