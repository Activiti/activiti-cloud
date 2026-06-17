/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.activiti.cloud.api.model.shared.QueryCloudVariableInstance;
import org.activiti.cloud.api.process.model.ProcessInstanceSearchResult;

public class ProcessInstanceSearchResultImpl extends CloudProcessInstanceImpl implements ProcessInstanceSearchResult {

    private long subprocessesCount;
    private long linkedProcessesCount;
    private String linkedProcessInstanceId;
    private String linkedProcessInstanceType;
    private String type;
    private Set<QueryCloudVariableInstance> variables = new LinkedHashSet<>();

    @Override
    public long getSubprocessesCount() {
        return subprocessesCount;
    }

    @Override
    public void setSubprocessesCount(long subprocessesCount) {
        this.subprocessesCount = subprocessesCount;
    }

    @Override
    public long getLinkedProcessesCount() {
        return linkedProcessesCount;
    }

    @Override
    public void setLinkedProcessesCount(long linkedProcessesCount) {
        this.linkedProcessesCount = linkedProcessesCount;
    }

    @Override
    public String getLinkedProcessInstanceId() {
        return linkedProcessInstanceId;
    }

    @Override
    public void setLinkedProcessInstanceId(String linkedProcessInstanceId) {
        this.linkedProcessInstanceId = linkedProcessInstanceId;
    }

    @Override
    public String getLinkedProcessInstanceType() {
        return linkedProcessInstanceType;
    }

    @Override
    public void setLinkedProcessInstanceType(String linkedProcessInstanceType) {
        this.linkedProcessInstanceType = linkedProcessInstanceType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Set<QueryCloudVariableInstance> getVariables() {
        return variables;
    }

    @Override
    public void setVariables(Set<QueryCloudVariableInstance> variables) {
        this.variables = variables;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProcessInstanceSearchResultImpl other = (ProcessInstanceSearchResultImpl) obj;
        return (
            subprocessesCount == other.subprocessesCount &&
            linkedProcessesCount == other.linkedProcessesCount &&
            Objects.equals(linkedProcessInstanceId, other.linkedProcessInstanceId) &&
            Objects.equals(linkedProcessInstanceType, other.linkedProcessInstanceType) &&
            Objects.equals(type, other.type)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(),
            subprocessesCount,
            linkedProcessesCount,
            linkedProcessInstanceId,
            linkedProcessInstanceType,
            type
        );
    }
}
