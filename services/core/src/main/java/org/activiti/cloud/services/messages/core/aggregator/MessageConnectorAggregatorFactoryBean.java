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

package org.activiti.cloud.services.messages.core.aggregator;

import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.TaskScheduler;


/**
 * {@link org.springframework.beans.factory.FactoryBean} to create an
 * {@link MessageConnectorAggregator}.
 *
 */
public class MessageConnectorAggregatorFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageConnectorAggregator> {

    private MessageGroupProcessor processorBean;

    private Boolean expireGroupsUponCompletion;

    private Long sendTimeout;

    private String outputChannelName;

    private AbstractMessageHandlerMetrics metrics;

    private Boolean statsEnabled;

    private Boolean countsEnabled;

    private LockRegistry lockRegistry;

    private MessageGroupStore messageStore;

    private CorrelationStrategy correlationStrategy;

    private ReleaseStrategy releaseStrategy;

    private Expression groupTimeoutExpression;

    private List<Advice> forceReleaseAdviceChain;

    private TaskScheduler taskScheduler;

    private MessageChannel discardChannel;

    private String discardChannelName;

    private Boolean sendPartialResultOnExpiry;

    private Long minimumTimeoutForEmptyGroups;

    private Boolean expireGroupsUponTimeout;

    private Boolean completeGroupsWhenEmpty;
    
    private Boolean popSequence;

    private Boolean releaseLockBeforeSend;
    
    public MessageConnectorAggregatorFactoryBean() {
        super();
        
        // defaults
        this.popSequence(false)
            .completeGroupsWhenEmpty(true)
            .expireGroupsUponCompletion(true)
            .sendPartialResultOnExpiry(true)
            .statsEnabled(true)
            .countsEnabled(true);
    }

    public MessageConnectorAggregatorFactoryBean processorBean(MessageGroupProcessor processorBean) {
        this.processorBean = processorBean;
        
        return this;
    }

    public MessageConnectorAggregatorFactoryBean expireGroupsUponCompletion(Boolean expireGroupsUponCompletion) {
        this.expireGroupsUponCompletion = expireGroupsUponCompletion;
        
        return this;
    }

    public MessageConnectorAggregatorFactoryBean sendTimeout(Long sendTimeout) {
        this.sendTimeout = sendTimeout;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean outputChannelName(String outputChannelName) {
        this.outputChannelName = outputChannelName;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean outputChannel(MessageChannel outputChannel) {
        this.setOutputChannel(outputChannel);

        return this;
    }

    public MessageConnectorAggregatorFactoryBean metrics(AbstractMessageHandlerMetrics metrics) {
        this.metrics = metrics;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean statsEnabled(Boolean statsEnabled) {
        this.statsEnabled = statsEnabled;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean countsEnabled(Boolean countsEnabled) {
        this.countsEnabled = countsEnabled;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean lockRegistry(LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean messageStore(MessageGroupStore messageStore) {
        this.messageStore = messageStore;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean correlationStrategy(CorrelationStrategy correlationStrategy) {
        this.correlationStrategy = correlationStrategy;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean releaseStrategy(ReleaseStrategy releaseStrategy) {
        this.releaseStrategy = releaseStrategy;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean groupTimeoutExpression(Expression groupTimeoutExpression) {
        this.groupTimeoutExpression = groupTimeoutExpression;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean forceReleaseAdviceChain(List<Advice> forceReleaseAdviceChain) {
        this.forceReleaseAdviceChain = forceReleaseAdviceChain;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean taskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean discardChannel(MessageChannel discardChannel) {
        this.discardChannel = discardChannel;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean discardChannelName(String discardChannelName) {
        this.discardChannelName = discardChannelName;
    
        return this;
    }

    public MessageConnectorAggregatorFactoryBean sendPartialResultOnExpiry(Boolean sendPartialResultOnExpiry) {
        this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean minimumTimeoutForEmptyGroups(Long minimumTimeoutForEmptyGroups) {
        this.minimumTimeoutForEmptyGroups = minimumTimeoutForEmptyGroups;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean expireGroupsUponTimeout(Boolean expireGroupsUponTimeout) {
        this.expireGroupsUponTimeout = expireGroupsUponTimeout;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean popSequence(Boolean popSequence) {
        this.popSequence = popSequence;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean releaseLockBeforeSend(Boolean releaseLockBeforeSend) {
        this.releaseLockBeforeSend = releaseLockBeforeSend;

        return this;
    }

    public MessageConnectorAggregatorFactoryBean completeGroupsWhenEmpty(Boolean completeGroupsWhenEmpty) {
        this.completeGroupsWhenEmpty = completeGroupsWhenEmpty;

        return this;
    }
    
    public MessageConnectorAggregatorFactoryBean beanFactory(BeanFactory beanFactory) {
        this.setBeanFactory(beanFactory);
        
        return this;
    }

    public MessageConnectorAggregatorFactoryBean adviceChain(List<? extends HandleMessageAdvice> adviceChain) {
        this.setAdviceChain(Arrays.asList(adviceChain.toArray(new HandleMessageAdvice[] {})));
        
        return this;
    }
    
    @Override
    protected MessageConnectorAggregator createHandler() {
        MessageConnectorAggregator aggregator = new MessageConnectorAggregator(this.processorBean);

        if (this.expireGroupsUponCompletion != null) {
            aggregator.setExpireGroupsUponCompletion(this.expireGroupsUponCompletion);
        }

        if (this.sendTimeout != null) {
            aggregator.setSendTimeout(this.sendTimeout);
        }

        if (this.outputChannelName != null) {
            aggregator.setOutputChannelName(this.outputChannelName);
        }

        if (this.metrics != null) {
            aggregator.configureMetrics(this.metrics);
        }

        if (this.statsEnabled != null) {
            aggregator.setStatsEnabled(this.statsEnabled);
        }

        if (this.countsEnabled != null) {
            aggregator.setCountsEnabled(this.countsEnabled);
        }

        if (this.lockRegistry != null) {
            aggregator.setLockRegistry(this.lockRegistry);
        }

        if (this.messageStore != null) {
            aggregator.setMessageStore(this.messageStore);
        }

        if (this.correlationStrategy != null) {
            aggregator.setCorrelationStrategy(this.correlationStrategy);
        }

        if (this.releaseStrategy != null) {
            aggregator.setReleaseStrategy(this.releaseStrategy);
        }

        if (this.groupTimeoutExpression != null) {
            aggregator.setGroupTimeoutExpression(this.groupTimeoutExpression);
        }

        if (this.forceReleaseAdviceChain != null) {
            aggregator.setForceReleaseAdviceChain(this.forceReleaseAdviceChain);
        }

        if (this.taskScheduler != null) {
            aggregator.setTaskScheduler(this.taskScheduler);
        }

        if (this.discardChannel != null) {
            aggregator.setDiscardChannel(this.discardChannel);
        }

        if (this.discardChannelName != null) {
            aggregator.setDiscardChannelName(this.discardChannelName);
        }

        if (this.sendPartialResultOnExpiry != null) {
            aggregator.setSendPartialResultOnExpiry(this.sendPartialResultOnExpiry);
        }

        if (this.minimumTimeoutForEmptyGroups != null) {
            aggregator.setMinimumTimeoutForEmptyGroups(this.minimumTimeoutForEmptyGroups);
        }

        if (this.expireGroupsUponTimeout != null) {
            aggregator.setExpireGroupsUponTimeout(this.expireGroupsUponTimeout);
        }

        if (this.completeGroupsWhenEmpty != null) {
            aggregator.setCompleteGroupsWhenEmpty(this.completeGroupsWhenEmpty);
        }
                
        if (this.popSequence != null) {
            aggregator.setPopSequence(this.popSequence);
        }

        if (this.releaseLockBeforeSend != null) {
            aggregator.setReleaseLockBeforeSend(this.releaseLockBeforeSend);
        }

        return aggregator;
    }

    @Override
    protected Class<? extends MessageHandler> getPreCreationHandlerType() {
        return MessageConnectorAggregator.class;
    }

}
