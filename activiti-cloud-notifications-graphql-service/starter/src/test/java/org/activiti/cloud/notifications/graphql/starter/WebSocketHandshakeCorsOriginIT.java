/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.notifications.graphql.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.handler.codec.http.HttpHeaderNames;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.activiti.cloud.notifications.graphql.GrapqhQLApplication;
import org.activiti.cloud.notifications.graphql.config.EngineEventsConfiguration;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.server.support.GraphQlWebSocketMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { GrapqhQLApplication.class })
@ContextConfiguration(
    classes = { EngineEventsConfiguration.class },
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Import(TestChannelBinderConfiguration.class)
class WebSocketHandshakeCorsOriginIT {

    static final String WS_GRAPHQL_URI = "/v2/ws/graphql";
    private static final String GRAPHQL_WS = "graphql-transport-ws";
    private static final String TESTADMIN = "testadmin";
    private static final String AUTHORIZATION = "Authorization";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private static final WebsocketClientSpec graphqlWsClientSpec = WebsocketClientSpec.builder()
        .protocols(GRAPHQL_WS)
        .build();

    @LocalServerPort
    private String port;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BuildProperties buildProperties;

    private String accessToken;

    @BeforeEach
    void setUp() {
        identityTokenProducer.withTestUser(TESTADMIN);
        HttpHeaders authHeaders = identityTokenProducer.authorizationHeaders();
        accessToken = authHeaders.getFirst(AUTHORIZATION);
    }

    @Test
    void handshakeAndConnectionInitSucceedWithNoOriginHeader() throws JacksonException {
        assertConnectionAckReceived(h -> {});
    }

    @Test
    void handshakeAndConnectionInitSucceedWithOriginMatchingRequestHost() throws JacksonException {
        assertConnectionAckReceived(h -> h.set(HttpHeaderNames.ORIGIN, "http://localhost:" + port));
    }

    @Test
    void handshakeAndConnectionInitSucceedWithBrowserLikeLocalhostOrigin() throws JacksonException {
        assertConnectionAckReceived(h -> h.set(HttpHeaderNames.ORIGIN, "http://localhost:4200"));
    }

    @Test
    void handshakeAndConnectionInitSucceedWithArbitraryCrossOrigin() throws JacksonException {
        assertConnectionAckReceived(h -> h.set(HttpHeaderNames.ORIGIN, "https://hxps-rc.studio.dev.app.hyland.com"));
    }

    private void assertConnectionAckReceived(Consumer<io.netty.handler.codec.http.HttpHeaders> headers)
        throws JacksonException {
        ReplayProcessor<String> output = ReplayProcessor.create();
        Map<String, Object> payload = Map.of(AUTHORIZATION, accessToken);
        String initMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionInit(payload));

        HttpClient.create()
            .baseUrl("ws://localhost:" + port)
            .headers(headers)
            .wiretap(true)
            .websocket(graphqlWsClientSpec)
            .uri(WS_GRAPHQL_URI)
            .handle((i, o) -> {
                o.sendString(Mono.just(initMessage)).then().subscribe();
                return i.aggregateFrames().receive().asString();
            })
            .take(1)
            .subscribeWith(output)
            .collectList()
            .subscribe();

        String ackMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionAck(null));

        StepVerifier.create(output).expectNext(ackMessage).expectComplete().verify(TIMEOUT);
    }
}
