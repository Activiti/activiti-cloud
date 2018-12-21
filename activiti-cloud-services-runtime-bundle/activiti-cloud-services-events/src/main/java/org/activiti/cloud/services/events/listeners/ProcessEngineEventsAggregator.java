/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.events.converter.CachingExecutionContext;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

public class ProcessEngineEventsAggregator extends BaseCommandContextEventsAggregator<CloudRuntimeEvent<?,?>, MessageProducerCommandContextCloseListener>{

    private final MessageProducerCommandContextCloseListener closeListener;

    public ProcessEngineEventsAggregator(MessageProducerCommandContextCloseListener closeListener) {
        this.closeListener = closeListener;
    }

    @Override
    protected Class<MessageProducerCommandContextCloseListener> getCloseListenerClass() {
        return MessageProducerCommandContextCloseListener.class;
    }

    @Override
    protected MessageProducerCommandContextCloseListener getCloseListener() {
        return closeListener;
    }

    @Override
    protected String getAttributeKey() {
        return MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS;
    }
    
    @Override
    public void add(CloudRuntimeEvent<?, ?> element) {
        super.add(element);

        CommandContext commandContext = getCurrentCommandContext();

        // Let's try resolve underlying execution Id
        String executionId = resolveExecutionId(element);

        // Let's find and cache ExecutionContext for executionId
        if(executionId != null && commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.EXECUTION_CONTEXT) == null) {
            ExecutionEntity executionEntity = commandContext.getExecutionEntityManager()
                                                            .findById(executionId);
            
            ExecutionContext executionContext = createExecutionContext(executionEntity);
            
            if (executionEntity != null) {
                commandContext.addAttribute(MessageProducerCommandContextCloseListener.EXECUTION_CONTEXT,
                                            executionContext);
            }
        }
    }
    
    protected ExecutionContext createExecutionContext(ExecutionEntity executionEntity) {
        return new CachingExecutionContext(executionEntity);
    }

    protected String resolveExecutionId(CloudRuntimeEvent<?, ?> element) {
        if(element instanceof CloudProcessRuntimeEvent) {
            return element.getEntityId();
        } else if(element instanceof CloudVariableEvent) {
            return ((VariableInstance) element.getEntity()).getProcessInstanceId();
        } else if(element instanceof CloudTaskRuntimeEvent) {
            return ((Task) element.getEntity()).getProcessInstanceId();
        }
        
        return null;
    }
    
}
