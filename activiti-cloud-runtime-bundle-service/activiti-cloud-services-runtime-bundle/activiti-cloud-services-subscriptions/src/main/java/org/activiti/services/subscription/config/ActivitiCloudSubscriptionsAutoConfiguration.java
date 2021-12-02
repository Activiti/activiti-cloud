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
package org.activiti.services.subscription.config;

import static org.activiti.services.subscriptions.behavior.BroadcastSignalEventActivityBehavior.DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME;
import org.activiti.bpmn.model.Signal;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.activiti.runtime.api.conf.ProcessRuntimeAutoConfiguration;
import org.activiti.runtime.api.signal.SignalPayloadEventListener;
import org.activiti.services.subscription.SignalSender;
import org.activiti.services.subscription.channel.BroadcastSignalEventHandler;
import org.activiti.services.subscription.channel.ProcessEngineSignalChannels;
import org.activiti.services.subscriptions.behavior.BroadcastSignalEventActivityBehavior;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

@Configuration
@PropertySource("classpath:config/signal-events-channels.properties")
@AutoConfigureBefore({ProcessRuntimeAutoConfiguration.class})
public class ActivitiCloudSubscriptionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "disabled", matchIfMissing = true)
    public BroadcastSignalEventHandler broadcastSignalEventHandler(RuntimeService runtimeService) {
        return new BroadcastSignalEventHandler(runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "disabled", matchIfMissing = true)
    public SignalPayloadEventListener signalSender(ProcessEngineSignalChannels processEngineSignalChannels) {
        return new SignalSender(processEngineSignalChannels.signalProducer());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "enabled")
    public SignalPayloadEventListener signalSenderBridge(StreamBridge streamBridge,
            @Value("${activiti.stream.cloud.functional.binding.signalProducer.name:signalProducer-out-0}") String signalProducerBindingName) {
        return new SignalSender(streamBridge, signalProducerBindingName);
    }

    @Bean(DEFAULT_THROW_SIGNAL_EVENT_BEAN_NAME)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    public IntermediateThrowSignalEventActivityBehavior broadcastSignalEventActivityBehavior(ApplicationEventPublisher eventPublisher,
            SignalEventDefinition signalEventDefinition,
            Signal signal) {
        return new BroadcastSignalEventActivityBehavior(eventPublisher,
                signalEventDefinition,
                signal);
    }

}
