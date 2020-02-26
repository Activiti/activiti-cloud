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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerMessageHandler;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLBrokerSubProtocolHandler;
import org.activiti.cloud.services.notifications.graphql.ws.transport.GraphQLSubscriptionExecutor;
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
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({GraphQL.class, EnableWebSocketMessageBroker.class})
@ConditionalOnProperty(name="spring.activiti.cloud.services.query.graphql.ws.enabled", matchIfMissing = true)
public class GraphQLWebSocketMessageBrokerAutoConfiguration {

    @Configuration
    @EnableWebSocket
    public static class DefaultGraphQLWebSocketMessageBrokerConfiguration 
                            extends DelegatingWebSocketMessageBrokerConfiguration {

        @Autowired
        private GraphQLWebSocketMessageBrokerConfigurationProperties configurationProperties;

        /**
         * A hook for subclasses to customize message broker configuration through the
         * provided {@link MessageBrokerRegistry} instance.
         */
        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker();
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
        @ConditionalOnMissingBean(GraphQLBrokerMessageHandler.class)
        public MessageHandler graphQLBrokerMessageHandler(SubscribableChannel clientInboundChannel,
                                                          MessageChannel clientOutboundChannel,
                                                          SubscribableChannel brokerChannel,
                                                          TaskScheduler messageBrokerTaskScheduler,
                                                          GraphQLSubscriptionExecutor graphQLSubscriptionExecutor) {
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
            handler.setDefaultProtocolHandler(graphQLBrokerSubProtocolHandler());

            return handler;
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphQLBrokerSubProtocolHandler graphQLBrokerSubProtocolHandler() {
            return new GraphQLBrokerSubProtocolHandler(configurationProperties.getEndpoint());
        }
        
        @Bean
        @ConditionalOnMissingBean
        public GraphQLSubscriptionExecutor graphQLSubscriptionExecutor(GraphQLSchema graphQLSchema) {
            return new GraphQLSubscriptionExecutor(graphQLSchema);
        }
        
        @Bean
        public ServletServerContainerFactoryBean createWebSocketContainer() {
            ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
            org.springframework.scheduling.concurrent.ConcurrentTaskExecutor f;
            
            container.setMaxTextMessageBufferSize(1024*64);
            container.setMaxBinaryMessageBufferSize(1024*10);
            container.setMaxSessionIdleTimeout(30000L);
            return container;
        }
    }
    
}