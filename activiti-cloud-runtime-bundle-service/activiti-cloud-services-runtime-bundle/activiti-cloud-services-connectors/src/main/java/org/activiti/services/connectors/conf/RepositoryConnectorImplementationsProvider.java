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

package org.activiti.services.connectors.conf;

import java.util.List;
import java.util.stream.Collectors;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.util.StringUtils;

public class RepositoryConnectorImplementationsProvider implements ConnectorImplementationsProvider {

    private final RepositoryService repositoryService;

    public RepositoryConnectorImplementationsProvider(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public List<String> getImplementations() {
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();

        return list
            .stream()
            .map(ProcessDefinition::getId)
            .map(repositoryService::getBpmnModel)
            .flatMap(model -> model.getProcesses().stream())
            .flatMap(process -> process.getFlowElements().stream())
            .filter(ServiceTask.class::isInstance)
            .map(ServiceTask.class::cast)
            .filter(task -> !StringUtils.hasText(task.getImplementationType()))
            .map(ServiceTask::getImplementation)
            .distinct()
            .collect(Collectors.toList());
    }
}
