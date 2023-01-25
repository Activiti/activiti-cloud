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

package org.activiti.cloud.services.query.rest;

public class RestDocConstants {

    public static final String ROOT_TASKS_DESC = "Filter tasks without parent task.";

    public static final String STANDALONE_TASKS_DESC = "Filter tasks without parent process.";

    public static final String PREDICATE_DESC = "Predicate binding to core entity parameter values.";

    public static final String PREDICATE_EXAMPLE = "{\"name\": \"Real name\"}";

    public static final String VARIABLE_KEYS_DESC = "Used to retrieve process variables. It is constructed " +
            "from process definition key and variable name, e.g.: {processDefinitionKey}/{variableName}.";

    public static final String VARIABLE_KEYS_EXAMPLE = "Process_90W_3nLpw/initializedVar";

    private RestDocConstants() {
    }

}
