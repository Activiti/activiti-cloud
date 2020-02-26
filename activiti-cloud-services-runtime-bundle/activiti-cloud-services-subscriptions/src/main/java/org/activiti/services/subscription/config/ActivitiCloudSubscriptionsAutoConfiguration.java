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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@EnableBinding(ProcessEngineSignalChannels.class)
@AutoConfigureBefore({ProcessRuntimeAutoConfiguration.class})
public class ActivitiCloudSubscriptionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BroadcastSignalEventHandler broadcastSignalEventHandler(BinderAwareChannelResolver resolver,
                                                                  RuntimeService runtimeService) {
        return new BroadcastSignalEventHandler(resolver,
                                              runtimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SignalPayloadEventListener signalSender(BinderAwareChannelResolver resolver) {
        return new SignalSender(resolver);
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
