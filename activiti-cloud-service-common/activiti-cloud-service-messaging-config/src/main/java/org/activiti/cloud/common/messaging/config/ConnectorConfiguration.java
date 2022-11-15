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

import java.util.Optional;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@Configuration
public class ConnectorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FunctionDefinitionPropertySource functionDefinitionPropertySource(ConfigurableEnvironment configurableEnvironment){
        return new FunctionDefinitionPropertySource(configurableEnvironment);
    }

    @Bean
    public BeanPostProcessor connectorBeanPostProcessor(DefaultListableBeanFactory beanFactory,
                                                        IntegrationFlowContext integrationFlowContext,
                                                        StreamFunctionProperties streamFunctionProperties,
                                                        StreamBridge streamBridge,
                                                        FunctionDefinitionPropertySource functionDefinitionPropertySource) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Connector.class.isInstance(bean)) {
                    String connectorName = beanName;
                    String functionName = connectorName + "Connector";
                    Connector connector = Connector.class.cast(bean);

                    functionDefinitionPropertySource.register(functionName);

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConnectorDefinition.class))
                            .ifPresent(functionDefinition -> {
                                Optional.of(functionDefinition.output())
                                        .filter(StringUtils::hasText)
                                        .ifPresent(output -> {
                                            streamFunctionProperties.getBindings()
                                                                    .put(functionName + "-out-0", output);
                                        });

                                Optional.of(functionDefinition.input())
                                        .filter(StringUtils::hasText)
                                        .ifPresent(input -> {
                                            streamFunctionProperties.getBindings()
                                                                    .put(functionName + "-in-0", input);
                                        });
                            });

                    IntegrationFlow flow = IntegrationFlows.from(ConnectorMessageFunction.class,
                                                 (gateway) -> gateway.beanName(functionName)
                                                                     .replyTimeout(0L))
                                           .log(LoggingHandler.Level.INFO,functionName + ".integrationRequest")
                                           .handle(String.class,
                                                   (request, headers) -> {
                                                       String result = connector.apply(request);

                                                       Message<String> response = MessageBuilder.withPayload(result)
                                                                                                .build();
                                                       String destination = headers.get("resultDestination", String.class);

                                                       if (StringUtils.hasText(destination)) {
                                                           streamBridge.send(destination,
                                                                             response);
                                                           return null;
                                                       }

                                                       return response;
                                                   })
                                           .log(LoggingHandler.Level.INFO,functionName + ".integrationResult")
                                           .bridge()
                                           .get();

                    integrationFlowContext.registration(flow)
                                          .register();
                }
                return bean;
            }
        };
    }

    public interface ConnectorMessageFunction extends Function<Message<String>,Message<String>> {}
}
