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

import java.util.Objects;
import java.util.Set;
import org.activiti.cloud.api.process.model.QueryCloudProcessInstance;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;

public class QueryCloudProcessInstanceImpl extends CloudProcessInstanceImpl implements QueryCloudProcessInstance {

    private Set<QueryCloudSubprocessInstance> subprocesses;

    @Override
    public Set<QueryCloudSubprocessInstance> getSubprocesses() {
        return subprocesses;
    }

    @Override
    public void setSubprocesses(Set<QueryCloudSubprocessInstance> subprocesses) {
        this.subprocesses = subprocesses;
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
        QueryCloudProcessInstanceImpl other = (QueryCloudProcessInstanceImpl) obj;
        return Objects.equals(subprocesses, other.subprocesses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subprocesses);
    }
}
