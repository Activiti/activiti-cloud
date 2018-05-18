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

import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import graphql.GraphQL;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayDestinationResolver;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayPublisherFactory;
import org.activiti.cloud.services.query.qraphql.ws.transport.GraphQLBrokerMessageHandler;
import org.activiti.cloud.services.query.qraphql.ws.transport.GraphQLBrokerSubProtocolHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({GraphQL.class, EnableWebSocketMessageBroker.class})
@ConditionalOnProperty(name="spring.activiti.cloud.services.query.graphql.ws.enabled", matchIfMissing = true)
public class GraphQLWebSocketMessageBrokerAutoConfiguration {

    @Configuration
    @EnableWebSocket
    @EnableWebSocketMessageBroker
    public static class DefaultGraphQLWebSocketMessageBrokerConfiguration extends GraphQLWebSocketMessageBrokerConfigurationSupport implements WebSocketMessageBrokerConfigurer {

        @Autowired
        private GraphQLWebSocketMessageBrokerConfigurationProperties configurationProperties;

        /**
         * A hook for subclasses to customize message broker configuration through the
         * provided {@link MessageBrokerRegistry} instance.
         */
        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry
                    .enableStompBrokerRelay()
                    .setRelayHost(configurationProperties.getRelayHost())
                    .setRelayPort(configurationProperties.getRelayPort())
                    .setClientLogin(configurationProperties.getClientLogin())
                    .setClientPasscode(configurationProperties.getClientPasscode())
                    .setSystemLogin(configurationProperties.getSystemLogin())
                    .setSystemPasscode(configurationProperties.getSystemPasscode())
            ;
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint(configurationProperties.getEndpoint())
                    .setHandshakeHandler(new DefaultHandshakeHandler())
                    .setAllowedOrigins(configurationProperties.getAllowedOrigins())
                    .addInterceptors(new HttpSessionHandshakeInterceptor())
            ;
        }

        @Bean
        @ConditionalOnMissingBean
        public ReactorNettyTcpStompClient stompClient() {
            ReactorNettyTcpStompClient stompClient = new ReactorNettyTcpStompClient(configurationProperties.getRelayHost(),
                                                                                    configurationProperties.getRelayPort());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            return stompClient;
        }

        @Bean
        @ConditionalOnMissingBean
        public StompRelayPublisherFactory stompRelayPublisherFactory(ReactorNettyTcpStompClient stompClient,
                                                                     StompRelayDestinationResolver stompRelayDestinationResolver) {
            return new StompRelayPublisherFactory(stompClient).login(configurationProperties.getClientLogin())
                                                              .passcode(configurationProperties.getClientPasscode())
                                                              .destinationResolver(stompRelayDestinationResolver);
        }

        @Bean
        @ConditionalOnMissingBean(GraphQLBrokerMessageHandler.class)
        public MessageHandler graphQLBrokerMessageHandler(SubscribableChannel clientInboundChannel,
                                                          MessageChannel clientOutboundChannel,
                                                          SubscribableChannel brokerChannel,
                                                          TaskScheduler messageBrokerTaskScheduler,
                                                          GraphQLExecutor graphQLSubscriptionExecutor) {
            GraphQLBrokerMessageHandler messageHandler = new GraphQLBrokerMessageHandler(clientInboundChannel,
                    clientOutboundChannel,
                    brokerChannel,
                    graphQLSubscriptionExecutor);

            messageHandler.setTaskScheduler(messageBrokerTaskScheduler)
                          .setBufferCount(configurationProperties.getBufferCount())
                          .setBufferTimeSpanMs(configurationProperties.getBufferTimeSpanMs());

            return messageHandler;
        }

        @Override
        @Bean
        @ConditionalOnMissingBean(SubProtocolWebSocketHandler.class)
        public WebSocketHandler subProtocolWebSocketHandler() {
            SubProtocolWebSocketHandler handler = new SubProtocolWebSocketHandler(clientInboundChannel(),
                                                                                  clientOutboundChannel());
            handler.addProtocolHandler(graphQLBrokerSubProtocolHandler());

            return handler;
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphQLBrokerSubProtocolHandler graphQLBrokerSubProtocolHandler() {
            return new GraphQLBrokerSubProtocolHandler(configurationProperties.getEndpoint());
        }

    }

}