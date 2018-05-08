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

import java.util.Date;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.delegate.TriggerableActivityBehavior;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class MQServiceTaskBehavior extends DefaultServiceTaskBehavior implements TriggerableActivityBehavior {

    private final IntegrationContextManager integrationContextManager;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ApplicationEventPublisher eventPublisher;

    public MQServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                 RuntimeBundleProperties runtimeBundleProperties,
                                 ApplicationEventPublisher eventPublisher,
                                 ApplicationContext applicationContext) {
        super(applicationContext);
        this.integrationContextManager = integrationContextManager;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution) {
        if(hasConnectorBean(execution)){
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
     * {@link IntegrationRequestSender#sendIntegrationRequest(IntegrationRequestEvent)} which is annotated with
     * {@link TransactionalEventListener} on phase {@link TransactionPhase#AFTER_COMMIT}.
     * @param execution the related execution
     * @param integrationContext the related integration context
     */
    private void publishSpringEvent(DelegateExecution execution,
                                      IntegrationContextEntity integrationContext) {
        IntegrationRequestEvent event = new IntegrationRequestEvent(execution,
                                                                    integrationContext,
                runtimeBundleProperties.getAppName(),
                runtimeBundleProperties.getAppVersion(),
                runtimeBundleProperties.getServiceName(),
                runtimeBundleProperties.getServiceFullName(),
                runtimeBundleProperties.getServiceType(),
                runtimeBundleProperties.getServiceVersion());

        eventPublisher.publishEvent(event);
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
