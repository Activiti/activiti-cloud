/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.function.router;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_ANONYMOUS_INPUT;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.FUNCTION_DEFINITION;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;
import static org.springframework.messaging.MessageHeaders.ERROR_CHANNEL;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.spring.integration.BarrierMessageHandlerBuilder;
import org.activiti.cloud.common.messaging.spring.integration.ExpressionAdviceBuilder;
import org.activiti.cloud.common.messaging.spring.integration.IdempotentInterceptorBuilder;
import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.RecoveryCallback;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AutoConfiguration(before = BinderFactoryAutoConfiguration.class)
@EnableIntegration
@ConditionalOnProperty("activiti.cloud.messaging.function-router.enabled")
@ConditionalOnProperty(name = "activiti.cloud.messaging.function-router.type", havingValue = "gateway")
public class FunctionRouterGatewayAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterGatewayAutoConfiguration.class);

    @Bean
    FunctionRouterBindingResolver functionRouterBindingResolver(
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties
    ) {
        return new FunctionRouterBindingResolver(streamFunctionProperties, bindingServiceProperties);
    }

    @Bean
    FunctionRouterMessageDestinationResolver functionRouterMessageDestinationResolver(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new FunctionRouterMessageDestinationResolver(messagingProperties);
    }

    @Bean
    FunctionRouterDestinationsProvider functionRouterDestinationsProvider(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new FunctionRouterDestinationsProvider(messagingProperties.getFunctionRouter());
    }

    @Bean
    Function<Flux<Message<?>>, Mono<Void>> functionRouterAnonymousConsumer(
        FunctionRouterGateway functionRouterGateway,
        FunctionRouterMessageDestinationResolver functionRouterMessageDestinationResolver,
        FunctionRouterDestinationsProvider functionRouterDestinationsProvider,
        RecoveryCallback<Object> functionRouterRecoveryCallback
    ) {
        return new FunctionRouterConsumer(
            functionRouterGateway,
            functionRouterMessageDestinationResolver,
            functionRouterDestinationsProvider,
            functionRouterRecoveryCallback,
            () -> FUNCTION_ROUTER_ANONYMOUS_INPUT
        );
    }

    @Bean
    Function<Flux<Message<?>>, Mono<Void>> functionRouterConsumer(
        FunctionRouterGateway functionRouterGateway,
        FunctionRouterMessageDestinationResolver functionRouterMessageDestinationResolver,
        FunctionRouterDestinationsProvider functionRouterDestinationsProvider,
        RecoveryCallback<Object> functionRouterRecoveryCallback
    ) {
        return new FunctionRouterConsumer(
            functionRouterGateway,
            functionRouterMessageDestinationResolver,
            functionRouterDestinationsProvider,
            functionRouterRecoveryCallback,
            () -> FUNCTION_ROUTER_INPUT
        );
    }

    @Bean
    RecoveryCallback<Object> functionRouterRecoveryCallback() {
        return new ErrorMessageSendingRecoverer();
    }

    @Bean
    IntegrationFlow functionRouterConsumerErrorFlow(
        FunctionRouterConsumerErrorRecoverer functionRouterConsumerErrorRecoverer
    ) {
        return flow ->
            flow
                .channel(ERROR_CHANNEL)
                .filter(FunctionRouterMessageDeliveryException.class::isInstance)
                .handle(functionRouterConsumerErrorRecoverer);
    }

    @Bean
    FunctionRouterConsumerErrorRecoverer functionRouterConsumerErrorRecoverer(
        MessageRecoverer republishMessageRecoverer
    ) {
        return new FunctionRouterConsumerErrorRecoverer(republishMessageRecoverer);
    }

    @Bean
    Consumer<ErrorMessage> functionRouterConsumerErrorHandler() {
        return new FunctionRouterConsumerErrorMessageHandler();
    }

    @Bean(name = FunctionRouterGateway.FUNCTION_ROUTER_GATEWAY_INPUT_CHANNEL)
    public MessageChannel functionRouterGatewayInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel functionRouterTriggerChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel functionRouterBarrierDiscardChannel() {
        return new DirectChannel();
    }

    @Bean
    public BarrierMessageHandler functionRouterBarrierHandler(
        @Value("${function-router.request-timeout:300s}") Duration requestTimeout,
        MessageChannel functionRouterBarrierDiscardChannel
    ) {
        return BarrierMessageHandlerBuilder.create(requestTimeout.toMillis())
            .outputProcessor(new FunctionRouterGatewayResultOutputProcessor())
            .discardChannel(functionRouterBarrierDiscardChannel)
            .build();
    }

    @Bean
    CorrelationIdMessageRouterFunction correlationIdMessageRouterFunction() {
        return new CorrelationIdMessageRouterFunction();
    }

    @Bean
    public IntegrationFlow functionRouterGatewayIntegrationFlow(
        MessageChannel functionRouterGatewayInputChannel,
        BarrierMessageHandler functionRouterBarrierHandler,
        IntegrationFlow functionRouterIntegrationFlow,
        CorrelationIdMessageRouterFunction correlationIdMessageRouterFunction
    ) {
        return IntegrationFlow.from(functionRouterGatewayInputChannel)
            .enrichHeaders(enricher -> enricher.correlationIdFunction(correlationIdMessageRouterFunction, false))
            .publishSubscribeChannel(pubSub ->
                pubSub
                    .subscribe(flow -> flow.to(functionRouterIntegrationFlow))
                    .subscribe(flow -> flow.handle(functionRouterBarrierHandler))
            )
            .get();
    }

    @Bean
    public IntegrationFlow functionRouterTriggerFlow(
        MessageChannel functionRouterTriggerChannel,
        BarrierMessageHandler functionRouterBarrierHandler
    ) {
        return IntegrationFlow.from(functionRouterTriggerChannel).trigger(functionRouterBarrierHandler).get();
    }

    @Bean
    public Advice functionRouterErrorHandlingAdvice() {
        return ExpressionAdviceBuilder.create()
            .onFailureExpressionString("#exception")
            .returnFailureExpressionResult(true)
            .trapException(true)
            .build();
    }

    @Bean
    FunctionRouterDefinitionsProvider functionRouterDefinitionsProvider(FunctionProperties functionProperties) {
        return new FunctionRouterDefinitionsProvider(functionProperties);
    }

    @Bean
    Supplier<String> functionRouterCorrelationHeaderAttribute() {
        return () -> CORRELATION_ID;
    }

    @Bean
    public IntegrationFlow functionRouterIntegrationFlow(
        MessageChannel functionRouterTriggerChannel,
        Advice functionRouterErrorHandlingAdvice,
        IdempotentReceiverInterceptor idempotentReceiverInterceptor,
        FunctionRouterMessageSplitterProcessor functionRouterMessageSplitterProcessor,
        FunctionRouterMessageHandler functionRouterMessageHandler,
        FunctionRouterResultsProcessor functionRouterResultsProcessor,
        FunctionRouterPartitionKeySelector functionRouterPartitionKeySelector,
        FunctionRouterDefinitionsProvider functionRouterDefinitionsProvider,
        Supplier<String> functionRouterCorrelationHeaderAttribute,
        StatefulRequestHandlerCircuitBreakerAdvice statefulRequestHandlerCircuitBreakerAdvice
    ) {
        return IntegrationFlow.from(MessageChannels.direct())
            .split(functionRouterMessageSplitterProcessor)
            .channel(channels ->
                channels
                    .partitioned(functionRouterDefinitionsProvider.get().size())
                    .partitionKey(functionRouterPartitionKeySelector)
            )
            .handle(functionRouterMessageHandler, spec ->
                spec
                    .advice(idempotentReceiverInterceptor)
                    .advice(functionRouterErrorHandlingAdvice)
                    .advice(statefulRequestHandlerCircuitBreakerAdvice)
            )
            .aggregate(aggregator ->
                aggregator
                    .correlationStrategy(
                        new HeaderAttributeCorrelationStrategy(functionRouterCorrelationHeaderAttribute.get())
                    )
                    .releaseStrategy(new SequenceSizeReleaseStrategy(false))
                    .outputProcessor(functionRouterResultsProcessor)
            )
            .channel(functionRouterTriggerChannel)
            .get();
    }

    @Bean
    StatefulRequestHandlerCircuitBreakerAdvice statefulRequestHandlerCircuitBreakerAdvice(
        FunctionRouterIdempotentInterceptorKeyStrategy functionRouterIdempotentInterceptorKeyStrategy
    ) {
        return new StatefulRequestHandlerCircuitBreakerAdvice(
            functionRouterIdempotentInterceptorKeyStrategy,
            "rootProcessInstanceId",
            FUNCTION_DEFINITION
        );
    }

    @Bean
    FunctionRouterResultsProcessor functionRouterResultsProcessor() {
        return new FunctionRouterResultsProcessor();
    }

    @Bean
    FunctionRouterMessageHandler functionRouterMessageHandler(
        RoutingFunction routingFunction,
        FunctionRouterMessageRouteSelector functionRouterMessageRouteSelector,
        ConcurrentMetadataStore functionRouterMetadataStore,
        FunctionRouterIdempotentInterceptorKeyStrategy functionRouterIdempotentInterceptorKeyStrategy
    ) {
        return new FunctionRouterMessageHandler(
            routingFunction,
            functionRouterMessageRouteSelector,
            functionRouterMetadataStore,
            functionRouterIdempotentInterceptorKeyStrategy
        );
    }

    @Bean
    FunctionRouterPartitionKeySelector functionRouterPartitionKeySelector(
        FunctionRouterDefinitionsProvider functionRouterDefinitionsProvider,
        FunctionRouterMessageRouteSelector functionRouterMessageRouteSelector
    ) {
        return new FunctionRouterPartitionKeySelector(
            functionRouterDefinitionsProvider,
            functionRouterMessageRouteSelector
        );
    }

    @Bean
    FunctionRouterMessageRouteSelector functionRouterMessageRouteSelector() {
        return new FunctionRouterMessageRouteSelector();
    }

    @Bean
    FunctionRouterMessageSplitterProcessor functionRouterMessageSplitterProcessor(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new FunctionRouterMessageSplitterProcessor(messagingProperties.getFunctionRouter());
    }

    @Bean
    FunctionRouterIdempotentInterceptorKeyStrategy functionRouterIdempotentInterceptorKeyStrategy() {
        return new FunctionRouterIdempotentInterceptorKeyStrategy();
    }

    @Bean
    public IdempotentReceiverInterceptor idempotentReceiverInterceptor(
        ConcurrentMetadataStore functionRouterMetadataStore,
        FunctionRouterIdempotentInterceptorKeyStrategy functionRouterIdempotentInterceptorKeyStrategy
    ) {
        return IdempotentInterceptorBuilder.create(functionRouterMetadataStore)
            .keyStrategy(functionRouterIdempotentInterceptorKeyStrategy)
            .throwExceptionOnRejection(false)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "function-router-gateway.metadata-store",
        havingValue = "simple",
        matchIfMissing = true
    )
    public ConcurrentMetadataStore functionRouterMetadataStore() {
        return new SimpleMetadataStore();
    }
}
