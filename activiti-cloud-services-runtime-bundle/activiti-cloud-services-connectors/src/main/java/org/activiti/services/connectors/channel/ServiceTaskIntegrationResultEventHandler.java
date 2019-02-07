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

package org.activiti.services.connectors.channel;

import java.util.Optional;

import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.core.common.model.connector.ActionDefinition;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.runtime.api.connector.ConnectorActionDefinitionFinder;
import org.activiti.runtime.api.connector.VariablesMatchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ProcessEngineIntegrationChannels.class)
public class ServiceTaskIntegrationResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationResultEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final MessageChannel auditProducer;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final ConnectorActionDefinitionFinder connectorActionDefinitionFinder;
    private final VariablesMatchHelper variablesMatchHelper;

    public ServiceTaskIntegrationResultEventHandler(RuntimeService runtimeService,
                                                    IntegrationContextService integrationContextService,
                                                    MessageChannel auditProducer,
                                                    RuntimeBundleProperties runtimeBundleProperties,
                                                    RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                                    ConnectorActionDefinitionFinder connectorActionDefinitionFinder,
                                                    VariablesMatchHelper variablesMatchHelper) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.auditProducer = auditProducer;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.connectorActionDefinitionFinder = connectorActionDefinitionFinder;
        this.variablesMatchHelper = variablesMatchHelper;
    }

    @StreamListener(ProcessEngineIntegrationChannels.INTEGRATION_RESULTS_CONSUMER)
    public void receive(IntegrationResult integrationResult) {
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(integrationResult.getIntegrationContext().getId());

        if (integrationContextEntity != null) {
            integrationContextService.deleteIntegrationContext(integrationContextEntity);

            if (runtimeService.createExecutionQuery().executionId(integrationContextEntity.getExecutionId()).list().size() > 0) {

                String implementation = integrationResult.getIntegrationContext().getConnectorType();
                Optional<ActionDefinition> actionDefinitionOptional = connectorActionDefinitionFinder.find(implementation);

                if (actionDefinitionOptional.isPresent()) {
                    runtimeService.trigger(integrationContextEntity.getExecutionId(),
                            variablesMatchHelper.match(integrationResult.getIntegrationContext().getOutBoundVariables(), actionDefinitionOptional.get().getOutputs()));
                } else {
                    runtimeService.trigger(integrationContextEntity.getExecutionId(),
                            integrationResult.getIntegrationContext().getOutBoundVariables());
                }
            } else {
                String message = "No task is in this RB is waiting for integration result with execution id `" +
                        integrationContextEntity.getExecutionId() +
                        ", flow node id `" + integrationResult.getIntegrationContext().getClientId() +
                        "`. The integration result for the integration context `" + integrationResult.getIntegrationContext().getId() + "` will be ignored.";
                LOGGER.debug(message);
            }
            sendAuditMessage(integrationResult);
        }
    }

    private void sendAuditMessage(IntegrationResult integrationResult) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationResultReceivedImpl integrationResultReceived = new CloudIntegrationResultReceivedImpl(integrationResult.getIntegrationContext());
            runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationResultReceived);
            Message<CloudIntegrationResultReceivedImpl> message = MessageBuilder.withPayload(
                    integrationResultReceived).build();

            auditProducer.send(message);
        }
    }
}
