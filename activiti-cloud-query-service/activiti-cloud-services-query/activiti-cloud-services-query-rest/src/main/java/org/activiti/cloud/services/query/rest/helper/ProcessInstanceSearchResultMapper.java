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
package org.activiti.cloud.services.query.rest.helper;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.activiti.cloud.api.model.shared.QueryCloudVariableInstance;
import org.activiti.cloud.api.process.model.ProcessInstanceSearchResult;
import org.activiti.cloud.api.process.model.impl.ProcessInstanceSearchResultImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;

public final class ProcessInstanceSearchResultMapper {

    private ProcessInstanceSearchResultMapper() {}

    public static ProcessInstanceSearchResult toResult(
        ProcessInstanceEntity entity,
        long subprocessesCount,
        long linkedProcessesCount
    ) {
        ProcessInstanceSearchResultImpl result = new ProcessInstanceSearchResultImpl();
        result.setId(entity.getId());
        result.setName(entity.getName());
        result.setStartDate(entity.getStartDate());
        result.setInitiator(entity.getInitiator());
        result.setBusinessKey(entity.getBusinessKey());
        result.setStatus(entity.getStatus());
        result.setProcessDefinitionId(entity.getProcessDefinitionId());
        result.setProcessDefinitionKey(entity.getProcessDefinitionKey());
        result.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        result.setProcessDefinitionName(entity.getProcessDefinitionName());
        result.setParentId(entity.getParentId());
        result.setRootProcessInstanceId(entity.getRootProcessInstanceId());
        result.setCompletedDate(entity.getCompletedDate());
        result.setSuspendedDate(entity.getSuspendedDate());
        result.setServiceName(entity.getServiceName());
        result.setServiceFullName(entity.getServiceFullName());
        result.setServiceVersion(entity.getServiceVersion());
        result.setServiceType(entity.getServiceType());
        result.setAppName(entity.getAppName());
        result.setAppVersion(entity.getAppVersion());
        result.setLinkedProcessInstanceId(entity.getLinkedProcessInstanceId());
        result.setLinkedProcessInstanceType(entity.getLinkedProcessInstanceType());
        result.setType(entity.getType());
        result.setSubprocessesCount(subprocessesCount);
        result.setLinkedProcessesCount(linkedProcessesCount);
        result.setVariables(
            Optional.ofNullable(entity.getVariables())
                .map(vars -> (Set<QueryCloudVariableInstance>) new LinkedHashSet<QueryCloudVariableInstance>(vars))
                .orElseGet(LinkedHashSet::new)
        );
        return result;
    }
}
