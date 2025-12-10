/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.context.ExecutionContext;

public class IncidentService {

    private final ProcessEngineChannels producer;
    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final ManagementService managementService;
    private final RuntimeService runtimeService;

    public IncidentService(
        ProcessEngineChannels producer,
        MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
        ManagementService managementService,
        RuntimeService runtimeService
    ) {
        this.producer = producer;
        this.messageBuilderIncidentsChainFactory = messageBuilderIncidentsChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.managementService = managementService;
        this.runtimeService = runtimeService;
    }

    public void sendIncidentViaCommand(IntegrationContext integrationContext, Exception exception) {
        var incidentMessage =
            this.managementService.executeCommand(
                    new CreateIncidentEventFromIntegrationCmd(
                        integrationContext,
                        exception,
                        this.runtimeService,
                        this.messageBuilderIncidentsChainFactory,
                        this.runtimeBundleInfoAppender
                    )
                );

        this.producer.auditProducer().send(incidentMessage);
    }

    public void createAndSendIncidentEvent(ExecutionContext rootExecutionContext, Exception exception) {
        var incidentMessage =
            this.managementService.executeCommand(
                    new CreateIncidentEventFromExecutionCmd(
                        rootExecutionContext,
                        exception,
                        this.messageBuilderIncidentsChainFactory,
                        this.runtimeBundleInfoAppender
                    )
                );

        this.producer.auditProducerIncidents().send(incidentMessage);
    }
}
