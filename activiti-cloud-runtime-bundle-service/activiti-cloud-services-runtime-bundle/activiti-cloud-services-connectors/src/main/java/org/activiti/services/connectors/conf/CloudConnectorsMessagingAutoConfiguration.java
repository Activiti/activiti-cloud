/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

package org.activiti.services.connectors.conf;

import java.util.List;
import java.util.function.Consumer;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.cmd.SetExecutionVariablesCmd;
import org.activiti.engine.impl.cmd.TriggerCmd;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.services.connectors.channel.AggregateIntegrationResultReceivedEventCmd;
import org.activiti.services.connectors.channel.CompositeCommand;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(BinderFactoryAutoConfiguration.class)
public class CloudConnectorsMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectorImplementationsProvider connectorDestinationsProvider(RepositoryService repositoryService) {
        return new RepositoryConnectorImplementationsProvider(repositoryService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectorDestinationMappingStrategy destinationMappingStrategy() {
        return new ConnectorDestinationMappingStrategy() {};
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectorMessagingDestinationsConfigurer connectorMessagingDestinationsConfigurer(BindingServiceProperties bindingServiceProperties,
            ConnectorImplementationsProvider destinationsProvider,
            ConnectorDestinationMappingStrategy destinationMappingStrategy) {
        return new ConnectorMessagingDestinationsConfigurer(destinationsProvider,
                destinationMappingStrategy,
                bindingServiceProperties);
    }

    @Bean
    public Consumer<IntegrationResult> integrationResultsConsumer(IntegrationContextService integrationContextService, RuntimeService runtimeService,
            ManagementService managementService, RuntimeBundleProperties runtimeBundleProperties, ProcessEngineEventsAggregator processEngineEventsAggregator) {
        return (integrationResult) -> {
            IntegrationContext integrationContext = integrationResult.getIntegrationContext();
            IntegrationContextEntity integrationContextEntity = integrationContextService.findById(integrationContext.getId());

            String executionId = integrationContext.getExecutionId();
            List<Execution> executions = runtimeService.createExecutionQuery()
                    .executionId(executionId)
                    .list();
            if (integrationContextEntity != null) {
                integrationContextService.deleteIntegrationContext(integrationContextEntity);

                if (executions.size() > 0) {
                    Execution execution = executions.get(0);

                    if (execution.getActivityId()
                            .equals(integrationContext.getClientId())) {
                        triggerIntegrationContextExecution(integrationContext, managementService, runtimeBundleProperties, processEngineEventsAggregator);
                        return;
                    } else {
                        //                        LOGGER.warn("Could not find matching activityId '{}' for integration result '{}' with executionId '{}'",
                        //                                integrationContext.getClientId(),
                        //                                integrationResult,
                        //                                execution.getId());
                    }
                } else {
                    String message = "No task is in this RB is waiting for integration result with execution id `" +
                            executionId +
                            ", flow node id `" + integrationContext.getClientId() +
                            "`. The integration result for the integration context `" + integrationContext.getId() + "` will be ignored.";
                    //                    LOGGER.warn(message);
                }
                managementService.executeCommand(new AggregateIntegrationResultReceivedEventCmd(
                        integrationContext, runtimeBundleProperties, processEngineEventsAggregator));
            }
        };
    }

    @Bean
    public Consumer integrationErrorsConsumer() {
        return (message) -> System.out.println("ok");
    }

    private void triggerIntegrationContextExecution(IntegrationContext integrationContext, ManagementService managementService,
            RuntimeBundleProperties runtimeBundleProperties, ProcessEngineEventsAggregator processEngineEventsAggregator) {
        managementService.executeCommand(
                CompositeCommand.of(
                        new SetExecutionVariablesCmd(integrationContext.getExecutionId(),
                                integrationContext.getOutBoundVariables(), true),
                        new TriggerCmd(integrationContext.getExecutionId(), null),
                        new AggregateIntegrationResultReceivedEventCmd(integrationContext,
                                runtimeBundleProperties, processEngineEventsAggregator)
                ));
    }

}
