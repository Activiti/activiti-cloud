/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.services.connectors.behavior;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.TriggerableActivityBehavior;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MQServiceTaskBehavior extends AbstractBpmnActivityBehavior implements TriggerableActivityBehavior {

    private static final String CONNECTOR_TYPE = "connectorType";
    private final IntegrationContextManager integrationContextManager;
    private final IntegrationProducerCommandContextCloseListener contextCloseListener;

    @Autowired
    public MQServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                 IntegrationProducerCommandContextCloseListener contextCloseListener) {
        this.integrationContextManager = integrationContextManager;
        this.contextCloseListener = contextCloseListener;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext currentCommandContext = getCurrentCommandContext();

        IntegrationContextEntity integrationContext = buildIntegrationContext(execution);
        integrationContextManager.insert(integrationContext);

        List<Message<IntegrationRequestEvent>> messages = currentCommandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS);
        if (messages != null) {
            messages.add(buildMessage(execution,
                                      integrationContext));
        } else {
            messages = new ArrayList<>();
            messages.add(buildMessage(execution,
                                      integrationContext));
            currentCommandContext.addAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS,
                                               messages);
        }

        if (!currentCommandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)) {
            currentCommandContext.addCloseListener(contextCloseListener);
        }
    }

    protected CommandContext getCurrentCommandContext() {
        return Context.getCommandContext();
    }

    private Message<IntegrationRequestEvent> buildMessage(DelegateExecution execution,
                                                          IntegrationContextEntity integrationContext) {
        IntegrationRequestEvent event = new IntegrationRequestEvent(execution.getProcessInstanceId(),
                                                                    execution.getProcessDefinitionId(),
                                                                    integrationContext.getExecutionId(),
                                                                    execution.getVariables());

        String implementation = ((ServiceTask) execution.getCurrentFlowElement()).getImplementation();
        return MessageBuilder.withPayload(event)
                .setHeader(CONNECTOR_TYPE,
                           implementation)
                .build();
    }

    private IntegrationContextEntity buildIntegrationContext(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = integrationContextManager.create();
        integrationContext.setExecutionId(execution.getId());
        integrationContext.setProcessInstanceId(execution.getProcessInstanceId());
        integrationContext.setProcessDefinitionId(execution.getProcessDefinitionId());
        integrationContext.setCreatedDate(new Date());
        return integrationContext;
    }

    @Override
    public void trigger(DelegateExecution execution,
                        String signalEvent,
                        Object signalData) {
        leave(execution);
    }
}
