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
package org.activiti.cloud.api.task.model.impl;

import java.util.List;
import java.util.Set;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.QueryCloudTask;

public class QueryCloudTaskImpl extends CloudTaskImpl implements QueryCloudTask {

    public String processDefinitionName;
    public List<TaskPermissions> permissions;
    public Set<? extends CloudVariableInstance> processVariables;

    public QueryCloudTaskImpl() {}

    public QueryCloudTaskImpl(CloudTask task) {
        super(task);
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    @Override
    public List<TaskPermissions> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(List<TaskPermissions> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Set<? extends CloudVariableInstance> getProcessVariables() {
        return processVariables;
    }
}
