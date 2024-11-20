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
    private List<String> processDefinitionKeys;
    private List<String> excludedProcessDefinitionIds;

    public SyncCloudProcessDefinitionsPayload() {}

    @Override
    public String getId() {
        return id;
    }

    public List<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public List<String> getExcludedProcessDefinitionIds() {
        return excludedProcessDefinitionIds;
    }

    public void setProcessDefinitionKeys(List<String> processDefinitionKeys) {
        this.processDefinitionKeys = List.copyOf(processDefinitionKeys);
    }

    public void setExcludedProcessDefinitionIds(List<String> excludedProcessDefinitionIds) {
        this.excludedProcessDefinitionIds = List.copyOf(excludedProcessDefinitionIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncCloudProcessDefinitionsPayload that)) return false;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(processDefinitionKeys, that.processDefinitionKeys) &&
            Objects.equals(excludedProcessDefinitionIds, that.excludedProcessDefinitionIds)
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SyncCloudProcessDefinitionsPayload payload) {
        return new Builder(payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, processDefinitionKeys, excludedProcessDefinitionIds);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SyncCloudProcessDefinitionsPayload{");
        sb.append("id='").append(id).append('\'');
        sb.append(", processDefinitionKeys=").append(processDefinitionKeys);
        sb.append(", excludedProcessDefinitionIds=").append(excludedProcessDefinitionIds);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private List<String> processDefinitionKeys;
        private List<String> excludedProcessDefinitionIds;

        public Builder() {}

        public Builder(SyncCloudProcessDefinitionsPayload other) {
            this.processDefinitionKeys = other.processDefinitionKeys;
            this.excludedProcessDefinitionIds = other.excludedProcessDefinitionIds;
        }

        public Builder processDefinitionKeys(List<String> processDefinitionKeys) {
            this.processDefinitionKeys = List.copyOf(processDefinitionKeys);
            return this;
        }

        public Builder excludedProcessDefinitionIds(List<String> excludedProcessDefinitionIds) {
            this.excludedProcessDefinitionIds = List.copyOf(excludedProcessDefinitionIds);
            return this;
        }

        public SyncCloudProcessDefinitionsPayload build() {
            SyncCloudProcessDefinitionsPayload syncCloudProcessDefinitionsPayload = new SyncCloudProcessDefinitionsPayload();
            syncCloudProcessDefinitionsPayload.excludedProcessDefinitionIds = this.excludedProcessDefinitionIds;
            syncCloudProcessDefinitionsPayload.processDefinitionKeys = this.processDefinitionKeys;
            return syncCloudProcessDefinitionsPayload;
        }
    }
}
