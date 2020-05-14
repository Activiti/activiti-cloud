/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.services.subscriptions.behavior;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.bpmn.model.Signal;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.context.ApplicationEventPublisher;

public class BroadcastSignalEventActivityBehavior extends IntermediateThrowSignalEventActivityBehavior {

    public static final String DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME = "defaultThrowSignalEventBehavior";

    private static final long serialVersionUID = 1L;

    private final ApplicationEventPublisher eventPublisher;

    public BroadcastSignalEventActivityBehavior(ApplicationEventPublisher eventPublisher, SignalEventDefinition signalEventDefinition,
                                                        Signal signal) {
        super(signalEventDefinition, signal);
        this.eventPublisher = eventPublisher;
    }

    public void execute(DelegateExecution execution) {
    	if (processInstanceScope) {
            super.execute(execution);
            return;
        }

        CommandContext commandContext = Context.getCommandContext();
        String eventSubscriptionName;
        if (signalEventName != null) {
             eventSubscriptionName = signalEventName;
        } else {
             Expression expressionObject = commandContext.getProcessEngineConfiguration().getExpressionManager().createExpression(signalExpression);
             eventSubscriptionName = expressionObject.getValue(execution).toString();
        }

        SignalPayload signalPayload = new SignalPayload(eventSubscriptionName, execution.getVariables());
        eventPublisher.publishEvent(signalPayload);

        Context.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution,
                true);

    }
}