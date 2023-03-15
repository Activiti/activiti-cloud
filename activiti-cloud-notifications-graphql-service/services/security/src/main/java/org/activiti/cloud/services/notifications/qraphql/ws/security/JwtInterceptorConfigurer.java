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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class JwtInterceptorConfigurer implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptorConfigurer.class);
    private static final String GRAPHQL_MESSAGE_TYPE = "graphQLMessageType";

    private final GraphQLAccessTokenVerifier tokenVerifier;
    private List<String> headerValues = Arrays.asList("connection_init", "start");
    private String headerName = GRAPHQL_MESSAGE_TYPE;

    public JwtInterceptorConfigurer(GraphQLAccessTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message,
                        SimpMessageHeaderAccessor.class
                    );
                    if (accessor != null) {
                        if (headerValues.contains(accessor.getHeader(headerName))) {
                            Optional
                                .ofNullable(accessor.getUser())
                                .filter(JWSAuthentication.class::isInstance)
                                .map(JWSAuthentication.class::cast)
                                .ifPresent(jWSAuthentication -> {
                                    try {
                                        logger.info(
                                            "Verifying Access Token for {}",
                                            accessor.getHeader(GRAPHQL_MESSAGE_TYPE)
                                        );
                                        tokenVerifier.verifyToken((String) jWSAuthentication.getCredentials());
                                    } catch (Exception e) {
                                        throw new BadCredentialsException("Invalid token", e);
                                    }
                                });
                        }
                    }
                    return message;
                }
            }
        );
    }

    public void setHeaderValues(List<String> headerValues) {
        this.headerValues = headerValues;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}
