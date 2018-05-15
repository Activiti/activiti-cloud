package org.activiti.cloud.services.query.qraphql.ws.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class EnableActivitiGraphQLWebSocketsSecurityTest {

    @Autowired
    private GraphQLSecurityWebSocketMessageBrokerConfigurer configuration;

    @EnableAutoConfiguration
    @SpringBootConfiguration
    @EnableActivitiGraphQLWebSocketsSecurity
    static class EnableActivitiGraphQLWebSocketsSecurityTestApplication {

    }

    @Test
    public void testContextLoads() {
        assertThat(configuration.getEndpoint()).isEqualTo("/ws/graphql");
        assertThat(configuration.getAuthorities()).isEqualTo(new String[]{"graphql-ws"});
        assertThat(configuration.sameOriginDisabled()).isTrue();
    }


}