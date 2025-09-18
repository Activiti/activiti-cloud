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
package org.activiti.cloud.common.messaging.config;

import static org.springframework.integration.handler.LoggingHandler.Level.DEBUG;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.common.messaging.util.FunctionTypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@AutoConfiguration(
    after = { BinderFactoryAutoConfiguration.class, FunctionBindingConfiguration.class },
    before = FunctionConfiguration.class
)
public class ConnectorConfiguration extends AbstractFunctionalBindingConfiguration {

    private static final Log logger = LogFactory.getLog(ConnectorConfiguration.class);

    public static final String CONNECTOR_BINDING_SELECTOR_DISCARD_FLOW = "connectorBindingSelectorDiscardFlow";
    public static final String CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL = "connectorBindingSelectorDiscardChannel";
    public static final String NULL_CHANNEL = "nullChannel";

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
        ActivitiCloudMessagingProperties messagingProperties
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

                            // Fix for ConsumerConnector: if this is a Consumer type, we need to handle it properly
                            FunctionRegistration functionRegistration;
                            if (ConsumerConnector.class.isInstance(bean)) {
                                // For ConsumerConnector, create registration without calling .type() if output type is null
                                functionRegistration = new FunctionRegistration(bean);

                                // Only set the type if it's valid and won't cause NullPointerException
                                if (functionType != null) {
                                    try {
                                        Type outputType = FunctionTypeUtils.getOutputType(functionType);
                                        if (outputType != null) {
                                            // If output type is not null, we can safely set the function type
                                            functionRegistration = functionRegistration.type(functionType);
                                        } else {
                                            // For Consumer types with null output, create a proper Consumer type
                                            Type inputType = FunctionTypeUtils.getInputType(functionType);
                                            if (inputType != null) {
                                                Type consumerType = FunctionTypeUtils.consumerType(inputType);
                                                functionRegistration = functionRegistration.type(consumerType);
                                            }
                                        }
                                    } catch (Exception e) {
                                        // If anything fails, just use the registration without type info
                                        logger.debug(
                                            "Could not set function type for ConsumerConnector, proceeding without type: " +
                                            e.getMessage()
                                        );
                                    }
                                }
                            } else {
                                // For regular Connector (Function type), use the original approach
                                functionRegistration = new FunctionRegistration(bean);
                                if (functionType != null) {
                                    try {
                                        functionRegistration = functionRegistration.type(functionType);
                                    } catch (Exception e) {
                                        logger.debug(
                                            "Could not set function type for Connector, proceeding without type: " +
                                            e.getMessage()
                                        );
                                    }
                                }
                            }

                            final var functionBeanName = registerFunctionRegistration(beanName, functionRegistration);

                            if (functionRouter.isEnabled()) {
                                functionRouter.register(connectorBinding.input(), functionBeanName);
                            }

                            responseDestination.set(connectorBinding.outputHeader());

                            GenericHandler<Message> handler = (message, headers) -> {
                                FunctionInvocationWrapper function = functionFromDefinition(beanName);
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
                                .log(LoggingHandler.Level.DEBUG, beanName + ".integrationRequest")
                                .filter(
                                    selector,
                                    filter ->
                                        filter
                                            .discardChannel(CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL)
                                            .throwExceptionOnRejection(false)
                                )
                                .filter(
                                    connectorType,
                                    filter ->
                                        filter
                                            .discardChannel(CONNECTOR_BINDING_SELECTOR_DISCARD_CHANNEL)
                                            .throwExceptionOnRejection(false)
                                )
                                .handle(Message.class, handler)
                                .log(LoggingHandler.Level.DEBUG, beanName + ".integrationResult")
                                .bridge()
                                .get();

                            String inputChannel = connectorBinding.input();

                            IntegrationFlow inputChannelFlow = IntegrationFlow
                                .from(inputChannel)
                                .gateway(connectorFlow, spec -> spec.replyTimeout(0L))
                                .get();

                            integrationFlowContext.registration(inputChannelFlow).register();
                        });
                }
                return bean;
            }
        };
    }
}
