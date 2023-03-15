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
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.repository.ProcessDefinition;

public class ExecutionContextInfoAppender {

    private final ExecutionContext executionContext;

    public ExecutionContextInfoAppender(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public CloudRuntimeEvent<?, ?> appendExecutionContextInfoTo(CloudRuntimeEventImpl<?, ?> event) {
        // inject execution context info
        if (executionContext != null) {
            ExecutionEntity processInstance = executionContext.getProcessInstance();
            ProcessDefinition processDefinition = executionContext.getProcessDefinition();

            if (processInstance != null) {
                event.setProcessInstanceId(processInstance.getId());
                event.setBusinessKey(processInstance.getBusinessKey());

                // Let's try extract parent info from super execution if exists
                if (processInstance.getSuperExecutionId() != null) {
                    Optional
                        .ofNullable(processInstance.getSuperExecution())
                        .ifPresent(superExecution ->
                            event.setParentProcessInstanceId(superExecution.getProcessInstanceId())
                        );
                }
            }

            if (processDefinition != null) {
                event.setProcessDefinitionId(processDefinition.getId());
                event.setProcessDefinitionKey(processDefinition.getKey());
                event.setProcessDefinitionVersion(processDefinition.getVersion());
            }
        }

        return event;
    }
}
