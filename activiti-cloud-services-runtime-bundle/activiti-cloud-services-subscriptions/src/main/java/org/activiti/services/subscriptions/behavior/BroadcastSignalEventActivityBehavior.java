package org.activiti.services.subscriptions.behavior;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.bpmn.model.Signal;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.activiti.services.subscriptions.behavior.BroadcastSignalEventActivityBehavior.DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component(DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME)
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