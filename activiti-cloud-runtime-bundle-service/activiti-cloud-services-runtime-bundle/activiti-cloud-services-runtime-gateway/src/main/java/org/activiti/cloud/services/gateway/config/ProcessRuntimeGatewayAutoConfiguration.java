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

package org.activiti.cloud.services.gateway.config;

import static org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannels.PROCESS_RUNTIME_GATEWAY_PRODUCER;
import static org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannels.PROCESS_RUNTIME_GATEWAY_RESULTS;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.activiti.api.model.shared.Result;
import org.activiti.cloud.services.gateway.ProcessRuntimeGateway;
import org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannelsConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.integration.config.IntegrationConverter;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;

@AutoConfiguration
@EnableConfigurationProperties(ProcessRuntimeGatewayProperties.class)
@PropertySource("classpath:process-runtime-gateway.properties")
@Import({ ProcessRuntimeGatewayChannelsConfiguration.class })
public class ProcessRuntimeGatewayAutoConfiguration {

    public static final String PROCESS_RUNTIME_GATEWAY_BEAN_NAME = "processRuntimeGateway";
    public static final String PROCESS_RUNTIME_GATEWAY_RESULT_CHANNEL_NAME = "processRuntimeGatewayResultChannelName";

    @Bean
    IntegrationFlow processRuntimeGatewayProducerFlow(
        ProcessRuntimeGatewayProperties properties,
        StreamBridge streamBridge,
        BindingServiceProperties bindingServiceProperties,
        HeaderChannelRegistry headerChannelRegistry
    ) {
        return IntegrationFlow
            .from(
                ProcessRuntimeGateway.class,
                gatewayProxySpec ->
                    gatewayProxySpec
                        .beanName(PROCESS_RUNTIME_GATEWAY_BEAN_NAME)
                        .replyTimeout(properties.getReplyTimeout().toMillis())
            )
            .enrichHeaders(headerEnricherSpec ->
                headerEnricherSpec
                    .headerChannelsToString()
                    .headerFunction(
                        PROCESS_RUNTIME_GATEWAY_RESULT_CHANNEL_NAME,
                        message -> headerChannelRegistry.channelToChannelName(message.getHeaders().getReplyChannel())
                    )
            )
            .handle(
                message ->
                    streamBridge.send(
                        bindingServiceProperties.getBindingDestination(PROCESS_RUNTIME_GATEWAY_PRODUCER),
                        message
                    ),
                messageHandlerSpec -> messageHandlerSpec.advice(new RequestHandlerRetryAdvice())
            )
            .get();
    }

    @Bean
    IntegrationFlow processRuntimeGatewayResultsFlow(HeaderChannelRegistry headerChannelRegistry) {
        return IntegrationFlow
            .from(PROCESS_RUNTIME_GATEWAY_RESULTS)
            .filter(
                Message.class,
                message ->
                    Optional
                        .ofNullable(message.getHeaders().get(PROCESS_RUNTIME_GATEWAY_RESULT_CHANNEL_NAME, String.class))
                        .map(headerChannelRegistry::channelNameToChannel)
                        .isPresent()
            )
            .route(Message.class, message -> message.getHeaders().get(PROCESS_RUNTIME_GATEWAY_RESULT_CHANNEL_NAME))
            .get();
    }

    @IntegrationConverter
    @Bean
    ConditionalGenericConverter processRuntimeGatewayResultConverter(ObjectMapper objectMapper) {
        final var jackson2JsonObjectMapper = new Jackson2JsonObjectMapper(objectMapper);

        return new ConditionalGenericConverter() {
            @Override
            public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
                return (
                    Result.class.isAssignableFrom(targetType.getObjectType()) && sourceType.getType() == byte[].class
                );
            }

            @Override
            public Set<ConvertiblePair> getConvertibleTypes() {
                return null;
            }

            @Override
            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
                try {
                    return jackson2JsonObjectMapper.fromJson(source, targetType.getType());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
