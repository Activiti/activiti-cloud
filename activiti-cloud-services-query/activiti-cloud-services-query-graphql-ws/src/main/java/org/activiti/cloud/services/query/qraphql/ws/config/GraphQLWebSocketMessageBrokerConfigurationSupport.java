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
package org.activiti.cloud.services.query.qraphql.ws.config;

import java.util.List;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

public abstract class GraphQLWebSocketMessageBrokerConfigurationSupport extends WebSocketMessageBrokerConfigurationSupport {

    /**
     * Override this method to add custom message converters.
     * @param messageConverters the list to add converters to, initially empty
     * @return {@code true} if default message converters should be added to list,
     * {@code false} if no more converters should be added.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        return true;
    }

    /**
     * Configure options related to the processing of messages received from and
     * sent to WebSocket clients.
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // Nothing
    };

    /**
     * Configure the {@link org.springframework.messaging.MessageChannel} used for
     * incoming messages from WebSocket clients. By default the channel is backed
     * by a thread pool of size 1. It is recommended to customize thread pool
     * settings for production use.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Nothing
    };

    /**
     * Configure the {@link org.springframework.messaging.MessageChannel} used for
     * outbound messages to WebSocket clients. By default the channel is backed
     * by a thread pool of size 1. It is recommended to customize thread pool
     * settings for production use.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Nothing
    };

    /**
     * Add resolvers to support custom controller method argument types.
     * <p>This does not override the built-in support for resolving handler
     * method arguments. To customize the built-in support for argument
     * resolution, configure {@code SimpAnnotationMethodMessageHandler} directly.
     * @param argumentResolvers the resolvers to register (initially an empty list)
     * @since 4.1.1
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // Nothing
    };

    /**
     * Add handlers to support custom controller method return value types.
     * <p>Using this option does not override the built-in support for handling
     * return values. To customize the built-in support for handling return
     * values, configure  {@code SimpAnnotationMethodMessageHandler} directly.
     * @param returnValueHandlers the handlers to register (initially an empty list)
     * @since 4.1.1
     */
    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        // Nothing
    };

}
