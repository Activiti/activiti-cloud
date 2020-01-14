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

package org.activiti.cloud.services.messages.core.integration;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_TYPE;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

import java.util.List;
import java.util.Objects;

import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.messages.core.aggregator.MessageConnectorAggregator;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.activiti.cloud.services.messages.core.correlation.Correlations;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.messaging.Message;

public class MessageConnectorIntegrationFlow extends IntegrationFlowAdapter {

    private static final String MESSAGE_GATEWAY = "messageGateway";
    private static final String AGGREGATOR = "aggregator";
    private static final String ENRICH_HEADERS = "enrichHeaders";
    private static final String FILTER_MESSAGE = "filterMessage";
    public final static String DISCARD_CHANNEL = "discardChannel";
    
    private final MessageConnectorProcessor processor;
    private final MessageConnectorAggregator aggregator;
    private final IdempotentReceiverInterceptor interceptor;
    private final HandleMessageAdvice[] advices;

    public MessageConnectorIntegrationFlow(MessageConnectorProcessor processor,
                                           MessageConnectorAggregator aggregator,
                                           IdempotentReceiverInterceptor interceptor,
                                           List<? extends HandleMessageAdvice> advices) {
        this.processor = processor;
        this.aggregator = aggregator;
        this.interceptor = interceptor;
        this.advices = advices.toArray(new HandleMessageAdvice[] {});
    }

    @Override
    protected IntegrationFlowDefinition<?> buildFlow() {
        return this.from(processor.input())
                   .gateway(flow -> flow.log(LoggingHandler.Level.DEBUG)
                                        .filter(Message.class,
                                                this::filterMessage,
                                                filterSpec -> filterSpec.id(FILTER_MESSAGE)
                                                                        .discardChannel(DISCARD_CHANNEL))
                                        .enrichHeaders(enricher -> enricher.id(ENRICH_HEADERS)
                                                                           .headerFunction(CORRELATION_ID, 
                                                                                           this::enrichHeaders))
                                        .transform(Transformers.fromJson(MessageEventPayload.class))
                                        .handle(this.aggregator(),
                                                handlerSpec -> handlerSpec.id(AGGREGATOR)
                                                                          .advice(advices))
                                        .channel(processor.output()),
                            flowSpec -> flowSpec.transactional()
                                                .id(MESSAGE_GATEWAY)
                                                .requiresReply(false)
                                                .async(true)
                                                .replyTimeout(0L)
                                                .advice(interceptor));
    }
    
    public AbstractMessageProducingHandler aggregator() {
        return this.aggregator;
    }
    
    @ServiceActivator
    public void aggregator(Message<?> message) {
        aggregator.handleMessage(message);
    }

    @Filter
    public boolean filterMessage(Message<?> message) {
        return Objects.nonNull(message.getHeaders()
                                      .get(MESSAGE_EVENT_TYPE));
    }

    @Transformer
    public String enrichHeaders(Message<?> message) {
        return Correlations.getCorrelationId(message);
    }
    
}
