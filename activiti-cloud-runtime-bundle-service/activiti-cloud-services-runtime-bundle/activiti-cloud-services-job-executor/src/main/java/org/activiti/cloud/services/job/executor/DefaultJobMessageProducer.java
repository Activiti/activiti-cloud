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

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;

import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class DefaultJobMessageProducer implements JobMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJobMessageProducer.class);

    private static final String ROUTING_KEY = "routingKey";

    private final StreamBridge streamBridge;
    private final ApplicationEventPublisher eventPublisher;
    private final JobMessageBuilderFactory jobMessageBuilderFactory;
    private final FunctionBindingConfiguration.BindingResolver bindingResolver;

    public DefaultJobMessageProducer(
        StreamBridge streamBridge,
        ApplicationEventPublisher eventPublisher,
        JobMessageBuilderFactory jobMessageBuilderFactory,
        FunctionBindingConfiguration.BindingResolver bindingResolver
    ) {
        this.streamBridge = streamBridge;
        this.eventPublisher = eventPublisher;
        this.jobMessageBuilderFactory = jobMessageBuilderFactory;
        this.bindingResolver = bindingResolver;
    }

    @Override
    public void sendMessage(@NonNull String bindingName, @NonNull Job job) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("requires active transaction synchronization");
        }

        Assert.hasLength(job.getId(), "job id must not be empty");
        Assert.hasLength(bindingName, "job binding name must not be empty");

        Message<String> message = jobMessageBuilderFactory
            .create(job)
            .withPayload(job.getId())
            .setHeader(ROUTING_KEY, bindingName)
            .setHeader(FUNCTION_DESTINATION, bindingResolver.getBindingDestination(bindingName))
            .build();

        // Let's send message right after the main transaction has successfully committed.
        TransactionSynchronizationManager.registerSynchronization(
            new JobMessageTransactionSynchronization(message, bindingName)
        );
    }

    class JobMessageTransactionSynchronization implements TransactionSynchronization {

        private final String bindingName;
        private final Message<String> message;

        public JobMessageTransactionSynchronization(Message<String> message, String bindingName) {
            this.bindingName = bindingName;
            this.message = message;
        }

        @Override
        public void afterCommit() {
            logger.debug("Sending job message '{}' via stream bridge to: {}", message, bindingName);

            try {
                boolean sent = streamBridge.send(bindingName, message);

                if (!sent) {
                    throw new MessageDispatchingException(message);
                }

                eventPublisher.publishEvent(new JobMessageSentEvent(message, bindingName));
            } catch (Exception cause) {
                logger.error("Sending job message {} failed due to error: {}", message, cause.getMessage());

                eventPublisher.publishEvent(new JobMessageFailedEvent(message, cause, bindingName));
            }
        }
    }
}
