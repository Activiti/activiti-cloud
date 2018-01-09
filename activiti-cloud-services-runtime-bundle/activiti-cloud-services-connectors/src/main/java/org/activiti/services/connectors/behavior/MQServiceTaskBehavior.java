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

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.delegate.TriggerableActivityBehavior;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class MQServiceTaskBehavior extends AbstractBpmnActivityBehavior implements TriggerableActivityBehavior {

    private final IntegrationContextManager integrationContextManager;
    private final ProcessEngineIntegrationChannels integrationChannels;

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ProcessEngineChannels processEngineChannels;


    @Autowired
    public MQServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                 ProcessEngineIntegrationChannels processEngineIntegrationChannels,
                                 RuntimeBundleProperties runtimeBundleProperties,
                                 ProcessEngineChannels processEngineChannels) {
        this.integrationContextManager = integrationContextManager;
        this.integrationChannels = processEngineIntegrationChannels;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.processEngineChannels = processEngineChannels;
    }

    @Override
    public void execute(DelegateExecution execution) {
        IntegrationContextEntity integrationContext = storeIntegrationContext(execution);

        IntegrationRequestSender integrationRequestSender = new IntegrationRequestSender( integrationChannels, runtimeBundleProperties, processEngineChannels, integrationContext, execution);
        registerTransactionSynchronization(integrationRequestSender);
    }

    protected void registerTransactionSynchronization(IntegrationRequestSender integrationRequestSender) {
        TransactionSynchronizationManager.registerSynchronization(integrationRequestSender);
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
