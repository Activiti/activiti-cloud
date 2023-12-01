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
package org.activiti.cloud.starter.rb.configuration;

import org.activiti.cloud.starter.rb.behavior.CloudActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.behavior.VariablesPropagator;
import org.activiti.engine.impl.event.EventSubscriptionPayloadMappingProvider;
import org.activiti.runtime.api.impl.ExtensionsVariablesMappingProvider;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.activiti.spring.process.ProcessVariablesInitiator;
import org.springframework.context.ApplicationContext;

public class SignalBehaviourConfigurer implements ProcessEngineConfigurationConfigurer {

    private ApplicationContext applicationContext;
    private ExtensionsVariablesMappingProvider variablesMappingProvider;
    private ProcessVariablesInitiator processVariablesInitiator;
    private EventSubscriptionPayloadMappingProvider eventSubscriptionPayloadMappingProvider;
    private final VariablesPropagator variablesPropagator;

    public SignalBehaviourConfigurer(
        ApplicationContext applicationContext,
        ExtensionsVariablesMappingProvider variablesMappingProvider,
        ProcessVariablesInitiator processVariablesInitiator,
        EventSubscriptionPayloadMappingProvider eventSubscriptionPayloadMappingProvider,
        VariablesPropagator variablesPropagator
    ) {
        this.applicationContext = applicationContext;
        this.variablesMappingProvider = variablesMappingProvider;
        this.processVariablesInitiator = processVariablesInitiator;
        this.eventSubscriptionPayloadMappingProvider = eventSubscriptionPayloadMappingProvider;
        this.variablesPropagator = variablesPropagator;
    }

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        processEngineConfiguration.setEventSubscriptionPayloadMappingProvider(eventSubscriptionPayloadMappingProvider);

        processEngineConfiguration.setActivityBehaviorFactory(
            new CloudActivityBehaviorFactory(
                applicationContext,
                variablesMappingProvider,
                processVariablesInitiator,
                variablesPropagator
            )
        );
    }
}
