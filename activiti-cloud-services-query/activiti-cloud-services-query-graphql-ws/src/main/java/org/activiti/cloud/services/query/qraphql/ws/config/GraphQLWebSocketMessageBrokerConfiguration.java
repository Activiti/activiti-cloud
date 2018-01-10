/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.ProcessEngineNotificationStompRelayDataFetcherDestinationResolver;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayPublisherFactory;
import org.activiti.cloud.services.query.qraphql.ws.transport.GraphQLBrokerMessageHandler;
import org.activiti.cloud.services.query.qraphql.ws.transport.GraphQLBrokerSubProtocolHandler;
import org.springframework.beans.factory.annotation.Value;
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
@EnableWebSocket
@EnableWebSocketMessageBroker
public class GraphQLWebSocketMessageBrokerConfiguration extends GraphQLWebSocketMessageBrokerConfigurationSupport
												implements  WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host:rabbitmq}")
    private String relayHost;

    private int relayPort = 61613;

    @Value("${spring.rabbitmq.username:guest}")
    private String login;

    @Value("${spring.rabbitmq.password:guest}")
    private String passcode;

    private String graphQLEndpoint = "/ws/graphql";

    private String graphQLAllowedOrigins = "*";

    /**
     * A hook for subclasses to customize message broker configuration through the
     * provided {@link MessageBrokerRegistry} instance.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry
            .enableStompBrokerRelay()
            .setRelayHost(relayHost)
            .setClientLogin(login)
            .setClientPasscode(passcode)
        ;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(graphQLEndpoint)
            .setHandshakeHandler(new DefaultHandshakeHandler())
            .setAllowedOrigins(graphQLAllowedOrigins)
            .addInterceptors(new HttpSessionHandshakeInterceptor())
            ;
    }

    @Bean
    public ReactorNettyTcpStompClient stompClient() {
        ReactorNettyTcpStompClient stompClient = new ReactorNettyTcpStompClient(relayHost, relayPort);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        return stompClient;
    }

    @Bean
    public StompRelayPublisherFactory stompRelayPublisherFactory(ReactorNettyTcpStompClient stompClient) {
        return new StompRelayPublisherFactory(stompClient)
            .login(login)
            .passcode(passcode)
            .destinationResolver(new ProcessEngineNotificationStompRelayDataFetcherDestinationResolver())
        ;
    }


	@Bean
	public MessageHandler graphQLBrokerMessageHandler(SubscribableChannel clientInboundChannel,
                                            			MessageChannel clientOutboundChannel,
                                            			SubscribableChannel brokerChannel,
                                            			TaskScheduler messageBrokerTaskScheduler,
                                            			GraphQLExecutor graphQLSubscriptionExecutor)
	{
		GraphQLBrokerMessageHandler messageHandler = new GraphQLBrokerMessageHandler(clientInboundChannel,
				clientOutboundChannel, brokerChannel, graphQLSubscriptionExecutor);

		messageHandler.setTaskScheduler(messageBrokerTaskScheduler);

		return messageHandler;
	}

    @Override
	@Bean
    public WebSocketHandler subProtocolWebSocketHandler() {
    	SubProtocolWebSocketHandler handler = new SubProtocolWebSocketHandler(clientInboundChannel(), clientOutboundChannel());
    	handler.addProtocolHandler(new GraphQLBrokerSubProtocolHandler());

        return handler;
    }

}