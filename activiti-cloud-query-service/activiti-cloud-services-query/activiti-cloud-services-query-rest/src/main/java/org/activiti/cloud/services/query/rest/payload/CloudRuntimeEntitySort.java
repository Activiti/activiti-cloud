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
package org.activiti.cloud.services.query.rest.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Collection;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.springframework.data.domain.Sort;

public record CloudRuntimeEntitySort(
    String field, Sort.Direction direction, boolean isProcessVariable, String processDefinitionKey, VariableType type
) {
    /**
     * This constructor's purpose is to make deserialization of 'direction' case-insensitive.
     */
    @JsonCreator
    public CloudRuntimeEntitySort(
        String field,
        String direction,
        boolean isProcessVariable,
        String processDefinitionKey,
        VariableType type
    ) {
        this(
            field,
            Sort.Direction.fromString(direction),
            isProcessVariable,
            processDefinitionKey,
            type == null ? null : VariableType.fromString(type.name())
        );
    }
}
