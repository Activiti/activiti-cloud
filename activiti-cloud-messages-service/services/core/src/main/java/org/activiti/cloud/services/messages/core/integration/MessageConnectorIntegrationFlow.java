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
package org.activiti.cloud.services.messages.core.integration;

import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.messages.core.aggregator.MessageConnectorAggregator;
import org.activiti.cloud.services.messages.core.config.MessageAggregatorProperties;
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
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Objects;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_TYPE;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

public class MessageConnectorIntegrationFlow extends IntegrationFlowAdapter {

    private static final String MESSAGE_GATEWAY = "messageGateway";
    private static final String AGGREGATOR = "aggregator";
    private static final String ENRICH_HEADERS = "enrichHeaders";
    private static final String FILTER_MESSAGE = "filterMessage";
    public static final String DISCARD_CHANNEL = "discardChannel";
    public static final String REPLY_CHANNEL = "replyChannel";
    public static final String ERROR_CHANNEL = "errorChannel";

    private final MessageConnectorAggregator aggregator;
    private final IdempotentReceiverInterceptor interceptor;
    private final HandleMessageAdvice[] advices;
    private final MessageAggregatorProperties properties;
    private final AbstractMessageRouter router;

    public MessageConnectorIntegrationFlow(MessageConnectorAggregator aggregator,
                                           IdempotentReceiverInterceptor interceptor,
                                           List<? extends HandleMessageAdvice> advices,
                                           MessageAggregatorProperties properties,
                                           AbstractMessageRouter router) {
        this.aggregator = aggregator;
        this.interceptor = interceptor;
        this.advices = advices.toArray(new HandleMessageAdvice[]{});
        this.properties = properties;
        this.router = router;
    }

    @Override
    protected IntegrationFlowDefinition<?> buildFlow() {
        return this.from("messageConnectorInput-in-0")
                   .headerFilter(properties.getInputHeadersToRemove())
                   .gateway(flow -> flow.log(LoggingHandler.Level.DEBUG)
                                        .enrichHeaders(enricher -> enricher.headerChannelsToString(properties.getHeaderChannelsTimeToLiveExpression()))
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
                                        .route(this.output()),
                            flowSpec -> flowSpec.transactional()
                                                .id(MESSAGE_GATEWAY)
                                                .requiresReply(false)
                                                .async(true)
                                                .replyTimeout(0L)
                                                .advice(interceptor));
    }

    public AbstractMessageRouter output() {
        return router;
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
