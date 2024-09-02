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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FilterOperator {
    @JsonProperty("eq")
    EQUALS,
    @JsonProperty("like")
    LIKE,
    @JsonProperty("gt")
    GREATER_THAN,
    @JsonProperty("gte")
    GREATER_THAN_OR_EQUAL,
    @JsonProperty("lt")
    LESS_THAN,
    @JsonProperty("lte")
    LESS_THAN_OR_EQUAL,
}
