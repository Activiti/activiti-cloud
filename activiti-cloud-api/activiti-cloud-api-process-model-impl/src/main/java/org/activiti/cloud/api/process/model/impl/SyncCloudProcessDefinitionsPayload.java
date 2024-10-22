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

package org.activiti.cloud.api.process.model.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.activiti.api.model.shared.Payload;

public class SyncCloudProcessDefinitionsPayload implements Payload {

    private final String id = UUID.randomUUID().toString();
    private List<String> excludedProcessDefinitionIds;

    public SyncCloudProcessDefinitionsPayload() {}

    public SyncCloudProcessDefinitionsPayload(List<String> excludedProcessDefinitionIds) {
        this.excludedProcessDefinitionIds = excludedProcessDefinitionIds;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<String> getExcludedProcessDefinitionIds() {
        return excludedProcessDefinitionIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncCloudProcessDefinitionsPayload that)) return false;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(excludedProcessDefinitionIds, that.excludedProcessDefinitionIds)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, excludedProcessDefinitionIds);
    }
}
