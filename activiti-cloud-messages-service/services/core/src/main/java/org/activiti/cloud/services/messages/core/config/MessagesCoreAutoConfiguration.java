/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.messages.core.config;

import static org.activiti.cloud.services.messages.core.integration.MessageConnectorIntegrationFlow.DISCARD_CHANNEL;

import java.util.List;
import java.util.Optional;

import org.activiti.cloud.services.messages.core.advice.MessageConnectorHandlerAdvice;
import org.activiti.cloud.services.messages.core.advice.MessageReceivedHandlerAdvice;
import org.activiti.cloud.services.messages.core.advice.SubscriptionCancelledHandlerAdvice;
import org.activiti.cloud.services.messages.core.aggregator.MessageConnectorAggregator;
import org.activiti.cloud.services.messages.core.aggregator.MessageConnectorAggregatorFactoryBean;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.activiti.cloud.services.messages.core.controlbus.ControlBusGateway;
import org.activiti.cloud.services.messages.core.integration.MessageConnectorIntegrationFlow;
import org.activiti.cloud.services.messages.core.integration.MessageEventHeaders;
import org.activiti.cloud.services.messages.core.processor.MessageGroupProcessorChain;
import org.activiti.cloud.services.messages.core.processor.MessageGroupProcessorHandlerChain;
import org.activiti.cloud.services.messages.core.processor.ReceiveMessagePayloadGroupProcessor;
import org.activiti.cloud.services.messages.core.processor.StartMessagePayloadGroupProcessor;
import org.activiti.cloud.services.messages.core.release.MessageGroupReleaseChain;
import org.activiti.cloud.services.messages.core.release.MessageGroupReleaseStrategyChain;
import org.activiti.cloud.services.messages.core.release.MessageSentReleaseHandler;
import org.activiti.cloud.services.messages.core.support.ChainBuilder;
import org.activiti.cloud.services.messages.core.support.LockTemplate;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * A Processor app that performs aggregation.
 *
 */
@Configuration
@EnableIntegration
@EnableBinding(MessageConnectorProcessor.class)
@EnableIntegrationManagement
@EnableConfigurationProperties(MessageAggregatorProperties.class)
@EnableTransactionManagement
@PropertySource("classpath:config/activiti-cloud-services-messages-core.properties")
public class MessagesCoreAutoConfiguration {

    private static final String MESSAGE_CONNECTOR_AGGREGATOR_FACTORY_BEAN = "messageConnectorAggregatorFactoryBean";
    private static final String CONTROL_BUS = "controlBus";
    private static final String CONTROL_BUS_FLOW = "controlBusFlow";
    private static final String MESSAGE_CONNECTOR_INTEGRATION_FLOW = "messageConnectorIntegrationFlow";
    
    @Autowired
    private MessageAggregatorProperties properties;
    
    @Bean
    @ConditionalOnMissingBean(name = CONTROL_BUS_FLOW)
    public IntegrationFlow controlBusFlow() {
        return IntegrationFlows.from(ControlBusGateway.class)
                               .controlBus(spec -> spec.id(CONTROL_BUS))
                               .get();
    }
    
    @Bean
    @DependsOn(MESSAGE_CONNECTOR_AGGREGATOR_FACTORY_BEAN)
    @ConditionalOnMissingBean(name = MESSAGE_CONNECTOR_INTEGRATION_FLOW)
    public IntegrationFlow messageConnectorIntegrationFlow(MessageConnectorProcessor processor,
                                                           MessageConnectorAggregator aggregator,
                                                           IdempotentReceiverInterceptor interceptor,
                                                           List<MessageConnectorHandlerAdvice> adviceChain) { 
        return new MessageConnectorIntegrationFlow(processor,
                                                   aggregator,
                                                   interceptor,
                                                   adviceChain);
    }
    
    @Bean
    @ConditionalOnMissingBean(name = DISCARD_CHANNEL)
    public MessageChannel discardChannel() {
        return MessageChannels.direct()
                              .get();
    }

    @Bean
    @ConditionalOnMissingBean(MessageConnectorAggregator.class)
    public MessageConnectorAggregatorFactoryBean messageConnectorAggregatorFactoryBean(CorrelationStrategy correlationStrategy,
                                                                                       ReleaseStrategy releaseStrategy,
                                                                                       MessageGroupProcessor processorBean,
                                                                                       MessageGroupStore messageStore,
                                                                                       LockRegistry lockRegistry,
                                                                                       BeanFactory beanFactory,
                                                                                       MessageChannel discardChannel) {
        return new MessageConnectorAggregatorFactoryBean().discardChannel(discardChannel)
                                                          .groupTimeoutExpression(this.properties.getGroupTimeout())
                                                          .lockRegistry(lockRegistry)
                                                          .correlationStrategy(correlationStrategy)
                                                          .releaseStrategy(releaseStrategy)
                                                          .beanFactory(beanFactory)
                                                          .processorBean(processorBean)
                                                          .messageStore(messageStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(LockRegistry lockRegistry) {
        return new LockTemplate(lockRegistry);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CorrelationStrategy correlationStrategy() {
        return new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID); 
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "metadataStoreKeyStrategy")
    public MessageProcessor<String> metadataStoreKeyStrategy() {
        return m -> Optional.ofNullable(m.getHeaders().get(MessageEventHeaders.MESSAGE_EVENT_ID))
                            .map(Object::toString)
                            .orElseGet(() -> m.getHeaders().getId()
                                                           .toString());
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "messageReceivedHandlerAdvice")
    public MessageConnectorHandlerAdvice messageReceivedHandlerAdvice(MessageGroupStore messageStore,
                                                                      CorrelationStrategy correlationStrategy,
                                                                      LockTemplate lockTemplate) {
        return new MessageReceivedHandlerAdvice(messageStore,
                                                correlationStrategy,
                                                lockTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(name = "subscriptionCancelledHandlerAdvice")
    public MessageConnectorHandlerAdvice subscriptionCancelledHandlerAdvice(MessageGroupStore messageStore,
                                                                            CorrelationStrategy correlationStrategy,
                                                                            LockTemplate lockTemplate) {
        return new SubscriptionCancelledHandlerAdvice(messageStore,
                                                      correlationStrategy,
                                                      lockTemplate);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetadataStoreSelector metadataStoreSelector(ConcurrentMetadataStore metadataStore,
                                                       MessageProcessor<String> metadataStoreKeyStrategy) {
        return new MetadataStoreSelector(metadataStoreKeyStrategy,
                                         metadataStore);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IdempotentReceiverInterceptor idempotentReceiverInterceptor(MetadataStoreSelector metadataStoreSelector) {
        IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(metadataStoreSelector);
        
        interceptor.setDiscardChannelName("errorChannel");
        
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageGroupProcessorChain messageGroupProcessorChain(MessageGroupStore messageGroupStore) {
        return ChainBuilder.of(MessageGroupProcessorChain.class)
                           .first(new StartMessagePayloadGroupProcessor(messageGroupStore))
                           .then(new ReceiveMessagePayloadGroupProcessor(messageGroupStore))
                           .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageGroupProcessor messageConnectorPayloadGroupProcessor(MessageGroupProcessorChain messageGroupProcessorChain) {
        return new MessageGroupProcessorHandlerChain(messageGroupProcessorChain);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageGroupReleaseChain messageGroupReleaseChain(MessageGroupStore messageGroupStore) {
        return ChainBuilder.of(MessageGroupReleaseChain.class)
                           .first(new MessageSentReleaseHandler())
                           .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ReleaseStrategy messageConnectorReleaseStrategy(MessageGroupReleaseChain messageGroupReleaseChain) {
        return new MessageGroupReleaseStrategyChain(messageGroupReleaseChain);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager() {
      return new PseudoTransactionManager();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageGroupStore messageStore() {
        return new SimpleMessageStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConcurrentMetadataStore metadataStore() {
        return new SimpleMetadataStore();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }    
    
    
}
