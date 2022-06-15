/*
 * Copyright 2010-2020 Alfresco Software, Ltd.
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

package org.activiti.services.connectors.channel;

import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.IntegrationRequestSender;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationEventPublisher;

public class IntegrationRequestReplayer {

    private final IntegrationContextService integrationContextService;
    private final RuntimeService runtimeService;
    private final IntegrationContextBuilder integrationContextBuilder;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final IntegrationRequestSender integrationRequestSender;

    public IntegrationRequestReplayer(
        IntegrationContextService integrationContextService,
        RuntimeService runtimeService,
        IntegrationContextBuilder integrationContextBuilder,
        IntegrationRequestBuilder integrationRequestBuilder,
        IntegrationRequestSender integrationRequestSender) {
        this.integrationContextService = integrationContextService;
        this.runtimeService = runtimeService;
        this.integrationContextBuilder = integrationContextBuilder;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.integrationRequestSender = integrationRequestSender;
    }

    public void replay(String integrationContextId) {
        final IntegrationContextEntity integrationContextEntity = integrationContextService.findById(
            integrationContextId);

        String executionId = integrationContextEntity.getExecutionId();
        List<Execution> executions = runtimeService.createExecutionQuery()
            .executionId(executionId)
            .list();
        if (integrationContextEntity != null) {
            integrationContextService.deleteIntegrationContext(integrationContextEntity);

            if (executions.size() > 0) {
                Execution execution = executions.get(0);

                if (execution.getActivityId()
                    .equals(integrationContextEntity.getFlowNodeId())) {
                    IntegrationContext integrationContext = integrationContextBuilder.from(integrationContextEntity,
                        (DelegateExecution) execution);
                    integrationRequestSender.sendIntegrationRequest(integrationRequestBuilder.build(integrationContext));

                } else {
                    throw new ActivitiException("Unable to replay integration context because it points to flowNode '" +
                        integrationContextEntity.getFlowNodeId() + "' while the related execution points to flowNode '" + execution.getActivityId() );
                }
            } else {
                String message = "Unable to replay integration request because no task is in this RB is waiting for integration result with execution id `" +
                    executionId +
                    ", flow node id `" + integrationContextEntity.getFlowNodeId() + "'";
                throw new ActivitiException(message);
            }
//            managementService.executeCommand(new AggregateIntegrationResultReceivedEventCmd(
//                integrationContextEntity, runtimeBundleProperties, processEngineEventsAggregator));
        }


    }

}
