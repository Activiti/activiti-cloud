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
package org.activiti.cloud.services.events.listeners;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudSequenceFlowEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.events.converter.CachingExecutionContext;
import org.activiti.cloud.services.events.converter.ExecutionContextInfoAppender;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

public class ProcessEngineEventsAggregator
    extends BaseCommandContextEventsAggregator<CloudRuntimeEvent<?, ?>, MessageProducerCommandContextCloseListener> {

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
        CommandContext commandContext = getCurrentCommandContext();

        // Let's try resolve underlying execution Id
        String executionId = resolveExecutionId(element);

        // Let's find and cache ExecutionContext for executionId
        ExecutionContext executionContext = resolveExecutionContext(commandContext, executionId);

        // Let's inject execution context info into event using event execution process context
        if (executionContext != null) {
            ExecutionContextInfoAppender executionContextInfoAppender = createExecutionContextInfoAppender(
                executionContext
            );

            CloudRuntimeEventImpl<?, ?> event = CloudRuntimeEventImpl.class.cast(element);

            element = executionContextInfoAppender.appendExecutionContextInfoTo(event);
        }

        super.add(element);
    }

    protected ExecutionContext resolveExecutionContext(CommandContext commandContext, String executionId) {
        if (executionId != null && commandContext.getGenericAttribute(executionId) == null) {
            ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findById(executionId);

            mayBeAddRootExecutionContext(commandContext, executionEntity);

            ExecutionContext executionContext = createExecutionContext(executionEntity);

            if (executionEntity != null) {
                commandContext.addAttribute(executionId, executionContext);
            }
        }

        return commandContext.getGenericAttribute(executionId);
    }

    protected void mayBeAddRootExecutionContext(CommandContext commandContext, ExecutionEntity executionEntity) {
        ExecutionContext rootExecutionContext = commandContext.getGenericAttribute(
            MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT
        );

        if (rootExecutionContext == null && executionEntity.getRootProcessInstanceId() != null) {
            ExecutionEntity rootProcessInstance = commandContext
                .getExecutionEntityManager()
                .findById(executionEntity.getRootProcessInstanceId());

            rootExecutionContext = createExecutionContext(rootProcessInstance);

            commandContext.addAttribute(
                MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT,
                rootExecutionContext
            );
        }
    }

    protected ExecutionContextInfoAppender createExecutionContextInfoAppender(ExecutionContext executionContext) {
        return new ExecutionContextInfoAppender(executionContext);
    }

    protected ExecutionContext createExecutionContext(ExecutionEntity executionEntity) {
        return new CachingExecutionContext(executionEntity);
    }

    protected String resolveExecutionId(CloudRuntimeEvent<?, ?> element) {
        if (element instanceof CloudProcessRuntimeEvent) {
            return element.getEntityId();
        } else if (element instanceof CloudVariableEvent) {
            return ((VariableInstance) element.getEntity()).getProcessInstanceId();
        } else if (element instanceof CloudTaskRuntimeEvent) {
            return ((Task) element.getEntity()).getProcessInstanceId();
        } else if (element instanceof CloudBPMNActivityEvent) {
            return ((BPMNActivity) element.getEntity()).getProcessInstanceId();
        } else if (element instanceof CloudSequenceFlowEvent) {
            return ((BPMNSequenceFlow) element.getEntity()).getProcessInstanceId();
        } else if (element instanceof CloudIntegrationEvent) {
            return ((CloudIntegrationEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudTaskCandidateUserEvent) {
            return ((CloudTaskCandidateUserEvent) element).getProcessInstanceId();
        } else if (element instanceof CloudTaskCandidateGroupEvent) {
            return ((CloudTaskCandidateGroupEvent) element).getProcessInstanceId();
        } else if (element instanceof CloudBPMNSignalEvent) {
            return ((CloudBPMNSignalEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudBPMNTimerEvent) {
            return ((CloudBPMNTimerEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudBPMNMessageEvent) {
            return ((CloudBPMNMessageEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudBPMNErrorReceivedEvent) {
            return ((CloudBPMNErrorReceivedEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudMessageSubscriptionCancelledEvent) {
            return ((CloudMessageSubscriptionCancelledEvent) element).getEntity().getProcessInstanceId();
        } else if (element instanceof CloudIntegrationEvent) {
            return ((CloudIntegrationEvent) element).getEntity().getProcessInstanceId();
        }
        return null;
    }
}
