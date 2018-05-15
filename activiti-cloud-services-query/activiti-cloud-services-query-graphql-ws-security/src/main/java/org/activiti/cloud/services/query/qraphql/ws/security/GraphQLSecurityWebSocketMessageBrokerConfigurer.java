package org.activiti.cloud.services.query.qraphql.ws.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

public class GraphQLSecurityWebSocketMessageBrokerConfigurer extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Value("${spring.activiti.cloud.services.graphql.ws.endpoint:/ws/graphql}")
    private String endpoint;

    @Value("${spring.activiti.cloud.services.graphql.ws.security.authorities:graphql-ws}")
    private String[] authorities;

    @Override
    protected boolean sameOriginDisabled() {
        return true; // disable CSRF for web sockets
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.simpMessageDestMatchers(endpoint)
                .hasAnyAuthority(authorities);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String[] getAuthorities() {
        return authorities;
    }
}
