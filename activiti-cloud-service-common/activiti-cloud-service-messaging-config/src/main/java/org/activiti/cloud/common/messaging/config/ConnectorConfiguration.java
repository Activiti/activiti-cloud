/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.config;

import static org.springframework.integration.handler.LoggingHandler.Level.DEBUG;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@AutoConfiguration(
    after = { BinderFactoryAutoConfiguration.class, FunctionBindingConfiguration.class },
    before = FunctionConfiguration.class
)
public class ConnectorConfiguration extends AbstractFunctionalBindingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfiguration.class);

    public static final String CONNECTOR_BINDING_SELECTOR_DISCARD_FLOW = "connectorBindingSelectorDiscardFlow";
    public static final String CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL = "connectorBindingSelectorDiscardChannel";
    public static final String NULL_CHANNEL = "nullChannel";
    public static final String RETRY_COUNT = "x-retry-count";

    @Bean(name = CONNECTOR_BINDING_SELECTOR_DISCARD_FLOW)
    IntegrationFlow functionBindingSelectorDiscardFlow() {
        return IntegrationFlow
            .from(CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL)
            .log(DEBUG, CONNECTOR_BINDING_SELECTOR_DISCARD_FLOW)
            .channel(NULL_CHANNEL)
            .get();
    }

    @Bean(name = "connectorBindingPostProcessor")
    public BeanPostProcessor connectorBindingPostProcessor(
        FunctionAnnotationService functionAnnotationService,
        IntegrationFlowContext integrationFlowContext,
        Function<String, String> resolveExpression,
        ActivitiCloudMessagingProperties messagingProperties,
        @Value("${activiti.connector.retry.default.max:-1}") int defaultMaxRetry,
        @Value("${activiti.connector.retry.default.delay:0}") Long defaultRetryDelay
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Connector.class.isInstance(bean) || ConsumerConnector.class.isInstance(bean)) {
                    final AtomicReference<String> responseDestination = new AtomicReference<>();

                    Optional
                        .ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, ConnectorBinding.class))
                        .ifPresent(connectorBinding -> {
                            final Type functionType = discoverFunctionType(bean, beanName);
                            final var functionRouter = messagingProperties.getFunctionRouter();

                            FunctionRegistration<Object> functionRegistration = new FunctionRegistration<>(bean)
                                .type(functionType);

                            final var functionDefinition = functionRouter.isEnabled()
                                ? beanName.concat("Target")
                                : beanName;

                            registerFunctionRegistration(functionDefinition, functionRegistration);

                            responseDestination.set(connectorBinding.outputHeader());

                            GenericHandler<Message> handler = (message, headers) -> {
                                FunctionInvocationWrapper function = functionFromDefinition(functionDefinition);
                                Object result = function.apply(message);

                                Message<?> response = null;
                                if (result != null) {
                                    response = MessageBuilder.withPayload(result).build();
                                    String destination = headers.get(responseDestination.get(), String.class);

                                    if (StringUtils.hasText(destination)) {
                                        getStreamBridge().send(destination, response);
                                        return null;
                                    }
                                }

                                return response;
                            };

                            GenericSelector<Message<?>> selector = Optional
                                .ofNullable(connectorBinding)
                                .map(ConnectorBinding::condition)
                                .filter(StringUtils::hasText)
                                .map(resolveExpression)
                                .map(ExpressionEvaluatingSelector::new)
                                .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                            GenericSelector<Message<?>> connectorType = Optional
                                .ofNullable(connectorBinding)
                                .map(ConnectorBinding::connectorType)
                                .filter(StringUtils::hasText)
                                .map(resolveExpression)
                                .map(it ->
                                    "headers.containsKey('connectorType') && headers['connectorType']=='" + it + "'"
                                )
                                .map(ExpressionEvaluatingSelector::new)
                                .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                            IntegrationFlow connectorFlow = IntegrationFlow
                                .from(
                                    getGatewayInterface(Function.class.isInstance(bean)),
                                    gateway -> gateway.replyTimeout(0L)
                                )
                                .log(DEBUG, beanName + ".integrationRequest")
                                .filter(
                                    selector,
                                    filter -> {
                                        int retry = connectorBinding.retry() != 0
                                            ? connectorBinding.retry()
                                            : defaultMaxRetry;
                                        if (retry > 0) {
                                            long retryDelay = connectorBinding.retryDelay() == 0
                                                ? defaultRetryDelay
                                                : connectorBinding.retryDelay();
                                            LOGGER.info(
                                                "Configure filter retry count to {} with delay {} for bean {}",
                                                retry,
                                                retryDelay,
                                                beanName
                                            );
                                            filter
                                                .discardFlow(flow -> handleRetryDiscardFlow(flow, retry, retryDelay))
                                                .throwExceptionOnRejection(false);
                                        } else {
                                            LOGGER.debug("Configure default discard for bean {}", beanName);
                                            filter
                                                .discardChannel(CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL)
                                                .throwExceptionOnRejection(false);
                                        }
                                    }
                                )
                                .filter(
                                    connectorType,
                                    filter ->
                                        filter
                                            .discardChannel(CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL)
                                            .throwExceptionOnRejection(false)
                                )
                                .handle(Message.class, handler)
                                .log(DEBUG, beanName + ".integrationResult")
                                .bridge()
                                .get();

                            String inputChannel = connectorBinding.input();

                            IntegrationFlow inputChannelFlow = IntegrationFlow
                                .from(inputChannel)
                                .channel(new QueueChannel(20))
                                .channel(new ExecutorChannel(Executors.newFixedThreadPool(5))) // parallelismo
                                .channel("connectorInputQueueChannel")
                                .gateway(connectorFlow, spec -> spec.replyTimeout(0L))
                                .get();

                            integrationFlowContext.registration(inputChannelFlow).register();

                            if (functionRouter.isEnabled()) {
                                final var functionBeanName = registerConnectorFlowFunction(connectorFlow, beanName);

                                Optional
                                    .ofNullable(connectorBinding.connectorType())
                                    .filter(StringUtils::hasText)
                                    .map(resolveExpression)
                                    .ifPresentOrElse(
                                        connectorTypeName ->
                                            functionRouter.register(
                                                connectorBinding.input(),
                                                functionBeanName,
                                                connectorTypeName
                                            ),
                                        () -> functionRouter.register(connectorBinding.input(), functionBeanName)
                                    );
                            }
                        });
                }
                return bean;
            }
        };
    }

    private void handleRetryDiscardFlow(IntegrationFlowDefinition<?> flow, int maxRetry, long retryDelay) {
        flow.handle((payload, headers) -> {
            Message<?> newMessage = handleMessagingExceptionIfPossible(payload, headers)
                .orElse(buildNewMessage(headers, payload));
            final var destination = headers.get("spring.cloud.function.destination", String.class);
            if (destination != null) {
                int retryCount = getRetryCount(headers);
                if (retryCount < maxRetry - 1) {
                    safeSleep(retryDelay);
                    getStreamBridge().send(destination, newMessage);
                } else {
                    LOGGER.error("Cannot retry message because retry limited exceeded: {}", maxRetry);
                }
            } else {
                LOGGER.error("Cannot retry message because destination from headers is null: {}", headers);
            }
            return null;
        });
    }

    private static void safeSleep(long retryDelay) {
        try {
            TimeUnit.SECONDS.sleep(retryDelay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Message<?>> handleMessagingExceptionIfPossible(Object payload, MessageHeaders headers) {
        if (payload instanceof MessagingException messagingException) {
            Object failedMessage = messagingException.getFailedMessage();
            if (failedMessage instanceof Message<?> originalMessage) {
                LOGGER.debug("Handling failed message for {}", payload);
                return Optional.of(buildNewMessage(headers, originalMessage.getPayload()));
            }
        }
        LOGGER.debug("Handled message exception for {}", payload);
        return Optional.empty();
    }

    private Message<?> buildNewMessage(MessageHeaders headers, Object payload) {
        int retryCount = handleRetryCount(headers);
        Message<Object> message = MessageBuilder
            .withPayload(payload)
            .copyHeaders(headers)
            .setHeader(RETRY_COUNT, retryCount)
            .build();
        LOGGER.info("New message for retry #{}: {}", retryCount, message);
        return message;
    }

    private int handleRetryCount(MessageHeaders headers) {
        int retryCount = getRetryCount(headers);
        retryCount++;
        return retryCount;
    }

    private static int getRetryCount(MessageHeaders headers) {
        return headers.getOrDefault(RETRY_COUNT, 0) instanceof Integer count ? count : 0;
    }
}
