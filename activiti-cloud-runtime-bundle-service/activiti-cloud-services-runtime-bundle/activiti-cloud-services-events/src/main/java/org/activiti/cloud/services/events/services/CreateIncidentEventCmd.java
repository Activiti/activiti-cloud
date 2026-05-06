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
package org.activiti.cloud.services.events.services;

import java.util.ArrayList;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.impl.IncidentContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.events.converter.ExecutionContextInfoAppender;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.Command;
import org.springframework.messaging.Message;

abstract class CreateIncidentEventCmd implements Command<Message> {

    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final IncidentSeverity severity;

    CreateIncidentEventCmd(
        MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender
    ) {
        this(messageBuilderIncidentsChainFactory, runtimeBundleInfoAppender, IncidentSeverity.ERROR);
    }

    CreateIncidentEventCmd(
        MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        IncidentSeverity severity
    ) {
        this.messageBuilderIncidentsChainFactory = messageBuilderIncidentsChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.severity = severity;
    }

    Message<ArrayList<Object>> createMessage(ExecutionContext rootExecutionContext, Exception exception) {
        var errorEvents = new ArrayList<>();
        var incident = createCloudIncidentCreatedEvent(rootExecutionContext, exception);
        errorEvents.add(incident);
        return this.messageBuilderIncidentsChainFactory.create(rootExecutionContext).withPayload(errorEvents).build();
    }

    private CloudIncidentCreatedEventImpl createCloudIncidentCreatedEvent(
        ExecutionContext rootExecutionContext,
        Exception exception
    ) {
        var incidentContext = getIncidentContext(rootExecutionContext);

        var incident = new CloudIncidentCreatedEventImpl(exception, incidentContext, this.severity);
        getExecutionContextInfoAppender(rootExecutionContext).appendExecutionContextInfoTo(incident);
        this.runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(incident);

        return incident;
    }

    private IncidentContextImpl getIncidentContext(ExecutionContext rootExecutionContext) {
        var incidentContext = new IncidentContextImpl();
        incidentContext.setProcessInstanceId(rootExecutionContext.getProcessInstance().getId());
        incidentContext.setProcessDefinitionId(rootExecutionContext.getProcessDefinition().getId());
        incidentContext.setActivityId(rootExecutionContext.getProcessInstance().getActivityId());
        incidentContext.setExecutionId(rootExecutionContext.getExecution().getId());
        return incidentContext;
    }

    private ExecutionContextInfoAppender getExecutionContextInfoAppender(ExecutionContext rootExecutionContext) {
        return new ExecutionContextInfoAppender(rootExecutionContext);
    }
}
