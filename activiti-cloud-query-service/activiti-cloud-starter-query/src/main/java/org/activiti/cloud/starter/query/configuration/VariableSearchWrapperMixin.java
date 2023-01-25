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
package org.activiti.cloud.starter.query.configuration;

import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;

@ParameterObject
public class VariableSearchWrapperMixin {

    private VariableSearchMixin variables;

    public VariableSearchMixin getVariables() {
        return variables;
    }

    private static class VariableSearchMixin {

        public static final String PARAM_DESCRIPTION_PREFIX = "Filters the task search results by task variable ";

        public static final String NAME_PARAM_DESCRIPTION = PARAM_DESCRIPTION_PREFIX + "name, required with value.";

        public static final String VALUE_PARAM_DESCRIPTION = PARAM_DESCRIPTION_PREFIX + "value, required with name.";

        @Parameter(description = NAME_PARAM_DESCRIPTION)
        private String name;

        @Parameter(description = VALUE_PARAM_DESCRIPTION)
        private String value;

        @Parameter(hidden = true)
        private String type;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }
    }

}
