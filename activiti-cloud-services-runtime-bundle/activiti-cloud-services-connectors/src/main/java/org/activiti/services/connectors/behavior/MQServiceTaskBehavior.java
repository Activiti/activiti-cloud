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

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.core.common.model.connector.ActionDefinition;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.delegate.TriggerableActivityBehavior;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.ConnectorActionDefinitionFinder;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.runtime.api.connector.VariablesMatchHelper;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Date;
import java.util.Optional;

public class MQServiceTaskBehavior extends DefaultServiceTaskBehavior implements TriggerableActivityBehavior {

    private final IntegrationContextManager integrationContextManager;
    private final ApplicationEventPublisher eventPublisher;
    private final IntegrationContextBuilder integrationContextBuilder;
    private final ConnectorActionDefinitionFinder connectorActionDefinitionFinder;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public MQServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                 ApplicationEventPublisher eventPublisher,
                                 ApplicationContext applicationContext,
                                 IntegrationContextBuilder integrationContextBuilder,
                                 ConnectorActionDefinitionFinder connectorActionDefinitionFinder,
                                 VariablesMatchHelper variablesMatchHelper,
                                 RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        super(applicationContext,
                integrationContextBuilder, connectorActionDefinitionFinder, variablesMatchHelper);
        this.integrationContextManager = integrationContextManager;
        this.eventPublisher = eventPublisher;
        this.integrationContextBuilder = integrationContextBuilder;
        this.connectorActionDefinitionFinder = connectorActionDefinitionFinder;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (hasConnectorBean(execution)) {
            // use de default implementation -> directly call a bean
            super.execute(execution);
        } else {
            IntegrationContextEntity integrationContext = storeIntegrationContext(execution);

            publishSpringEvent(execution,
                    integrationContext);
        }
    }

    /**
     * Publishes an custom event using the Spring {@link ApplicationEventPublisher}. This event will be caught by
     * {@link IntegrationRequestSender#sendIntegrationRequest(IntegrationRequest)} which is annotated with
     * {@link TransactionalEventListener} on phase {@link TransactionPhase#AFTER_COMMIT}.
     *
     * @param execution          the related execution
     * @param integrationContext the related integration context
     */
    private void publishSpringEvent(DelegateExecution execution,
                                      IntegrationContextEntity integrationContext) {

        String implementation = ((ServiceTask) execution.getCurrentFlowElement()).getImplementation();

        Optional<ActionDefinition> actionDefinitionOptional = connectorActionDefinitionFinder.find(implementation);

        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContextBuilder.from(integrationContext,
                execution, actionDefinitionOptional.orElse(null)));

        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationRequest);

        eventPublisher.publishEvent(integrationRequest);
    }


    private IntegrationContextEntity storeIntegrationContext(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = buildIntegrationContext(execution);
        integrationContextManager.insert(integrationContext);
        return integrationContext;
    }

    private IntegrationContextEntity buildIntegrationContext(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = integrationContextManager.create();
        integrationContext.setExecutionId(execution.getId());
        integrationContext.setProcessInstanceId(execution.getProcessInstanceId());
        integrationContext.setProcessDefinitionId(execution.getProcessDefinitionId());
        integrationContext.setFlowNodeId(execution.getCurrentActivityId());
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
