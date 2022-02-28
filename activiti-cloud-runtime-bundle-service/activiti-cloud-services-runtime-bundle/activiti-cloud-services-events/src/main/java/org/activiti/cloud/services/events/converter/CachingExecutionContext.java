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
package org.activiti.cloud.services.events.converter;

import java.util.Optional;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.repository.ProcessDefinition;

/**
 * Wraps ExecutionContext to cache parent method lookups to reduce
 * unnecessary db queries to lookup entities
 *
 */
public class CachingExecutionContext extends ExecutionContext {

    private volatile DeploymentEntity deploymentEntity = null;
    private volatile ExecutionEntity execution = null;
    private volatile ExecutionEntity processInstance = null;
    private volatile ProcessDefinition processDefinition = null;

    public CachingExecutionContext(ExecutionEntity execution) {
        super(execution);
    }

    @Override
    public DeploymentEntity getDeployment() {
        return Optional
            .ofNullable(deploymentEntity)
            .orElseGet(() -> {
                return (this.deploymentEntity = super.getDeployment());
            });
    }

    @Override
    public ExecutionEntity getExecution() {
        return Optional
            .ofNullable(execution)
            .orElseGet(() -> {
                return (this.execution = super.getExecution());
            });
    }

    @Override
    public ExecutionEntity getProcessInstance() {
        return Optional
            .ofNullable(processInstance)
            .orElseGet(() -> {
                return (this.processInstance = super.getProcessInstance());
            });
    }

    @Override
    public ProcessDefinition getProcessDefinition() {
        return Optional
            .ofNullable(processDefinition)
            .orElseGet(() -> {
                return (this.processDefinition = super.getProcessDefinition());
            });
    }
}
