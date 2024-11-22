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

import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;
import org.activiti.cloud.api.process.model.impl.QueryCloudSubprocessInstanceImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessInstanceControllerHelper {

    private final ProcessInstanceRepository processInstanceRepository;

    public ProcessInstanceControllerHelper(
            ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    public Page<ProcessInstanceEntity> mapSubprocesses(Page<ProcessInstanceEntity> processInstances,
            Pageable pageable) {
        List<String> parentIds = processInstances
                .getContent()
                .stream()
                .map(ProcessInstanceEntity::getId)
                .collect(Collectors.toList());

        Page<ProcessInstanceEntity> subprocesses = processInstanceRepository.findSubprocessesByParentIds(parentIds,
                pageable);

        Map<String, Set<QueryCloudSubprocessInstance>> subprocessMap = subprocesses.getContent().stream()
                .collect(Collectors.groupingBy(ProcessInstanceEntity::getParentId, Collectors.mapping(
                        subprocess -> {
                            QueryCloudSubprocessInstance subProcessInstance = new QueryCloudSubprocessInstanceImpl();
                            subProcessInstance.setId(subprocess.getId());
                            subProcessInstance.setProcessDefinitionName(subprocess.getProcessDefinitionName());
                            return subProcessInstance;
                        },
                        Collectors.toSet())));

        processInstances.getContent().forEach(processInstance -> {
            Set<QueryCloudSubprocessInstance> subprocessSet = subprocessMap.getOrDefault(processInstance.getId(),
                    Set.of());
            processInstance.setSubprocesses(subprocessSet);
        });

        return processInstances;
    }
}
