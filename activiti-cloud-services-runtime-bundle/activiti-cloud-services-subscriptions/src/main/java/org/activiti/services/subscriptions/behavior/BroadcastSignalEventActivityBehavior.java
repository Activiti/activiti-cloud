package org.activiti.services.subscriptions.behavior;

import org.activiti.bpmn.model.Signal;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.activiti.spring.bpmn.parser.CloudActivityBehaviorFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope("prototype")
@Component(CloudActivityBehaviorFactory.DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME)
public class BroadcastSignalEventActivityBehavior extends IntermediateThrowSignalEventActivityBehavior {
    
    private static final long serialVersionUID = 1L;

    private final ApplicationEventPublisher eventPublisher;

    public BroadcastSignalEventActivityBehavior(ApplicationEventPublisher eventPublisher, SignalEventDefinition signalEventDefinition,
                                                        Signal signal) {
        super(signalEventDefinition, signal);
        this.eventPublisher = eventPublisher;
    }

    public void execute(DelegateExecution execution) {
        super.execute(execution);

        CommandContext commandContext = Context.getCommandContext();
        String eventSubscriptionName = null;
        if (signalEventName != null) {
            eventSubscriptionName = signalEventName;
        } else {
            Expression expressionObject = commandContext.getProcessEngineConfiguration().getExpressionManager().createExpression(signalExpression);
            eventSubscriptionName = expressionObject.getValue(execution).toString();
        }

        SignalPayload signalPayload = new SignalPayload(eventSubscriptionName, execution.getVariables());
        eventPublisher.publishEvent(signalPayload);
    }
}