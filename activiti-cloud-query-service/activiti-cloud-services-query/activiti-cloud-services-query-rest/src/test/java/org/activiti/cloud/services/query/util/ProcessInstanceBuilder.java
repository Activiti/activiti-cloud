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
package org.activiti.cloud.services.query.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;

public class ProcessInstanceBuilder {

    private final ProcessInstanceEntity process;
    private final Set<TaskBuilder> taskBuffer = new HashSet<>();

    private final VariableRepository variableRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    public ProcessInstanceBuilder(
        VariableRepository variableRepository,
        ProcessInstanceRepository processInstanceRepository
    ) {
        this.variableRepository = variableRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.process = new ProcessInstanceEntity();
        this.process.setId(UUID.randomUUID().toString());
        this.process.setName(UUID.randomUUID().toString());
        this.withProcessDefinitionKey(UUID.randomUUID().toString());
    }

    public ProcessInstanceBuilder withId(String id) {
        process.setId(id);
        return this;
    }

    public ProcessInstanceBuilder withName(String name) {
        process.setName(name);
        return this;
    }

    public ProcessInstanceBuilder withAppName(String appName) {
        process.setAppName(appName);
        return this;
    }

    public ProcessInstanceBuilder withProcessDefinitionKey(String processDefinitionKey) {
        process.setProcessDefinitionKey(processDefinitionKey);
        return this;
    }

    public ProcessInstanceBuilder withAppVersion(String appVersion) {
        process.setAppVersion(appVersion);
        return this;
    }

    public ProcessInstanceBuilder withLastModified(Date lastModified) {
        process.setLastModified(lastModified);
        return this;
    }

    public ProcessInstanceBuilder withStartDate(Date startDate) {
        process.setStartDate(startDate);
        return this;
    }

    public ProcessInstanceBuilder withCompletedDate(Date completedDate) {
        process.setCompletedDate(completedDate);
        return this;
    }

    public ProcessInstanceBuilder withSuspendedDate(Date suspendedDate) {
        process.setSuspendedDate(suspendedDate);
        return this;
    }

    public ProcessInstanceBuilder withVariables(QueryTestUtils.VariableInput... variables) {
        process.setVariables(
            Arrays
                .stream(variables)
                .map(variable -> {
                    ProcessVariableEntity processVariable = new ProcessVariableEntity();
                    processVariable.setProcessInstanceId(process.getId());
                    processVariable.setProcessDefinitionKey(process.getProcessDefinitionKey());
                    processVariable.setName(variable.name());
                    processVariable.setType(variable.type().name().toLowerCase());
                    processVariable.setValue(variable.value());
                    return processVariable;
                })
                .collect(Collectors.toSet())
        );
        return this;
    }

    public ProcessInstanceBuilder withTasks(TaskBuilder... tasks) {
        taskBuffer.addAll(Arrays.asList(tasks));
        return this;
    }

    public ProcessInstanceBuilder withInitiator(String initiator) {
        process.setInitiator(initiator);
        return this;
    }

    public ProcessInstanceBuilder withStatus(ProcessInstance.ProcessInstanceStatus status) {
        process.setStatus(status);
        return this;
    }

    public ProcessInstanceEntity buildAndSave() {
        variableRepository.saveAll(process.getVariables());
        process.setTasks(
            taskBuffer
                .stream()
                .map(builder -> builder.withParentProcess(process))
                .map(TaskBuilder::buildAndSave)
                .collect(Collectors.toSet())
        );
        return processInstanceRepository.save(process);
    }
}
