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
package org.activiti.cloud.services.job.executor;

import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class DefaultJobMessageProducer implements JobMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJobMessageProducer.class);

    private static final String ROUTING_KEY = "routingKey";

    private final BinderAwareChannelResolver resolver;
    private final ApplicationEventPublisher eventPublisher;
    private final JobMessageBuilderFactory jobMessageBuilderFactory;

    public DefaultJobMessageProducer(
        BinderAwareChannelResolver resolver,
        ApplicationEventPublisher eventPublisher,
        JobMessageBuilderFactory jobMessageBuilderFactory
    ) {
        this.resolver = resolver;
        this.eventPublisher = eventPublisher;
        this.jobMessageBuilderFactory = jobMessageBuilderFactory;
    }

    @Override
    public void sendMessage(@NonNull String destination, @NonNull Job job) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }

        Assert.hasLength(job.getId(), "job id must not be empty");
        Assert.hasLength(destination, "destination must not be empty");

        Message<String> message = jobMessageBuilderFactory
            .create(job)
            .withPayload(job.getId())
            .setHeader(ROUTING_KEY, destination)
            .build();

        // Let's try to resolve message channel while inside main Activiti transaction to minimize infrastructure errors
        MessageChannel messageChannel = resolver.resolveDestination(destination);

        // Let's send message right after the main transaction has successfully committed.
        TransactionSynchronizationManager.registerSynchronization(
            new JobMessageTransactionSynchronization(message, messageChannel)
        );
    }

    class JobMessageTransactionSynchronization implements TransactionSynchronization {

        private final MessageChannel messageChannel;
        private final Message<String> message;

        public JobMessageTransactionSynchronization(Message<String> message, MessageChannel messageChannel) {
            this.messageChannel = messageChannel;
            this.message = message;
        }

        @Override
        public void afterCommit() {
            logger.debug("Sending job message '{}' via message channel: {}", message, messageChannel);

            try {
                boolean sent = messageChannel.send(message);

                if (!sent) {
                    throw new MessageDispatchingException(message);
                }

                eventPublisher.publishEvent(new JobMessageSentEvent(message, messageChannel));
            } catch (Exception cause) {
                logger.error("Sending job message {} failed due to error: {}", message, cause.getMessage());

                eventPublisher.publishEvent(new JobMessageFailedEvent(message, cause, messageChannel));
            }
        }
    }
}
