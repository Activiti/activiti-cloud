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
package org.activiti.cloud.notifications.graphql.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.introproventures.graphql.jpa.query.web.GraphQLController.GraphQLQueryRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFailedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerRetriesDecrementedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageSentEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageWaitingEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerExecutedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFailedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerRetriesDecrementedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.notifications.graphql.GrapqhQLApplication;
import org.activiti.cloud.notifications.graphql.config.EngineEvents;
import org.activiti.cloud.notifications.graphql.config.EngineEventsConfiguration;
import org.activiti.cloud.services.notifications.graphql.web.api.GraphQLQueryResult;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.server.support.GraphQlWebSocketMessage;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.test.StepVerifier;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = { GrapqhQLApplication.class },
    properties = {
        "spring.graphql.websocket.path=" + ActivitiGraphQLWsNativeStarterIT.WS_GRAPHQL_URI,
        "spring.graphql.websocket.keep-alive=PT1S",
    }
)
@ContextConfiguration(
    classes = { EngineEventsConfiguration.class },
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Import(TestChannelBinderConfiguration.class)
public class ActivitiGraphQLWsNativeStarterIT {

    public static final String WS_GRAPHQL_URI = "/v2/ws/graphql";
    private static final String GRAPHQL_WS = "graphql-transport-ws";
    private static final String HRUSER = "hruser";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TESTADMIN = "testadmin";
    private static final String TESTDEVOPS = "testdevops";
    private static final String TASK_NAME = "task1";
    private static final String GRAPHQL_URL = "/graphql";
    private static final Duration TIMEOUT = Duration.ofMillis(20000);

    private static final WebsocketClientSpec graphqlWsClientSpec = WebsocketClientSpec
        .builder()
        .protocols(GRAPHQL_WS)
        .build();

    @LocalServerPort
    private String port;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private EngineEvents producerChannel;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    private GraphQlTester graphQlTester;

    @BeforeEach
    public void setUp() throws Exception {
        identityTokenProducer.withTestUser(TESTADMIN);
        authHeaders = identityTokenProducer.authorizationHeaders();
        authHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        URI url = getUrl(WS_GRAPHQL_URI);
        this.graphQlTester =
            WebSocketGraphQlTester
                .builder(url, new ReactorNettyWebSocketClient())
                .interceptor(new JwtGraphQlClientInterceptor(identityTokenProducer.withTestUser(TESTADMIN)))
                .build();
    }

    protected URI getUrl(String path) throws URISyntaxException {
        return new URI("ws://localhost:" + this.port + path);
    }

    @Test
    public void testGraphqlWsSubprotocolConnectionInitXAuthorizationSupported() throws JsonProcessingException {
        testConnectionInit(TESTADMIN);
    }

    @Test
    public void testGraphqlWsSubprotocolServerWithUserRoleAuthorized() throws JsonProcessingException {
        testConnectionInit(HRUSER);
    }

    private void testConnectionInit(String user) throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        identityTokenProducer.withTestUser(user);
        final String accessToken = identityTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> payload = new StringObjectMapBuilder().put("Authorization", accessToken).get();

        var initMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionInit(payload));

        HttpClient
            .create()
            .baseUrl("ws://localhost:" + port)
            .wiretap(true)
            .websocket(graphqlWsClientSpec)
            .uri(WS_GRAPHQL_URI)
            .handle((i, o) -> {
                o.sendString(Mono.just(initMessage)).then().log("client-send").subscribe();

                return i
                    .aggregateFrames()
                    .receive()
                    .asString()
                    .doOnCancel(() -> {
                        closeWebSocketAnCompleteDataProcessor(output, o);
                    });
            })
            .log("client-received")
            .take(2)
            .subscribeWith(output)
            .collectList()
            .subscribe();

        var ackMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionAck(null));

        var kaMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.ping(null));

        StepVerifier.create(output).expectNext(ackMessage).expectNext(kaMessage).expectComplete().verify(TIMEOUT);
    }

    @Test
    public void testGraphqlWsSubprotocolServerWithUserRoleUnauthorized() throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        identityTokenProducer.withTestUser(TESTDEVOPS);
        final String accessToken = identityTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> payload = new StringObjectMapBuilder().put("Authorization", accessToken).get();

        var initMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionInit(payload));

        HttpClient
            .create()
            .baseUrl("ws://localhost:" + port)
            .wiretap(true)
            .websocket(graphqlWsClientSpec)
            .uri(WS_GRAPHQL_URI)
            .handle((i, o) -> {
                o.sendString(Mono.just(initMessage)).then().log("client-send").subscribe();

                return i
                    .aggregateFrames()
                    .receive()
                    .asString()
                    .doOnCancel(() -> {
                        closeWebSocketAnCompleteDataProcessor(output, o);
                    });
            })
            .log("client-received")
            .take(1)
            .subscribeWith(output)
            .collectList()
            .subscribe();

        StepVerifier.create(output).expectComplete().verify(); //TODO add timeout
    }

    @Test
    void testGraphqlSubscription_PROCESS_CREATED_and_PROCESS_STARTED() {
        Map<String, Object> variables = mapBuilder()
            .put("appName", "default-app")
            .put("eventTypes", Arrays.array("PROCESS_CREATED", "PROCESS_STARTED"))
            .get();

        var document =
            """
            subscription($appName: String!, $eventTypes: [EngineEventType!]) {
              engineEvents(appName: [$appName], eventType: $eventTypes) {
                processInstanceId
                eventType
              }
            }""";

        CloudProcessCreatedEvent event1 = new CloudProcessCreatedEventImpl() {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessInstanceId("processInstanceId");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudProcessStartedEvent event2 = new CloudProcessStartedEventImpl() {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceType("runtime-bundle");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessInstanceId("processInstanceId");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        var messages = List.of(
            Map.of("processInstanceId", "processInstanceId", "eventType", "PROCESS_CREATED"),
            Map.of("processInstanceId", "processInstanceId", "eventType", "PROCESS_STARTED")
        );

        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);

        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(sendEvents(event1, event2))
            .expectNext(messages)
            .thenCancel()
            .verify(TIMEOUT);
    }

    private Runnable sendEvents(CloudRuntimeEvent... events) {
        return () ->
            producerChannel
                .output()
                .send(
                    MessageBuilder.withPayload(Arrays.array(events)).setHeader("routingKey", "eventProducer").build()
                );
    }

    private void closeWebSocketAnCompleteDataProcessor(
        ReplayProcessor<String> data,
        WebsocketOutbound webSocketOutbound
    ) {
        webSocketOutbound.sendClose().doOnTerminate(data::onComplete).subscribe();
    }

    @Test
    public void testGraphqlSubscriptionPROCESS_DEPLOYED() {
        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app").get();

        var document =
            """
            subscription($appName: String!) {
              engineEvents(appName: [$appName], eventType: PROCESS_DEPLOYED) {
                processDefinitionKey
                eventType
              }
            }""";

        CloudProcessDeployedEvent event1 = new CloudProcessDeployedEventImpl(
            "id",
            new Date().getTime(),
            new ProcessDefinitionEntity()
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setProcessModelContent("processModelContent");
                setBusinessKey("businessKey");
            }
        };

        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);

        var messages = List.of(Map.of("processDefinitionKey", "processDefinitionKey", "eventType", "PROCESS_DEPLOYED"));
        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(sendEvents(event1))
            .expectNext(messages)
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionSIGNAL_RECEIVED() {
        Map<String, Object> variables = new StringObjectMapBuilder()
            .put("appName", "default-app")
            .put("eventType", "SIGNAL_RECEIVED")
            .get();

        var document =
            """
            subscription($appName: String!, $eventType: EngineEventType!) {
              engineEvents(appName: [$appName], eventType: [$eventType]) {
                processInstanceId
                processDefinitionId
                eventType
              }
            }""";

        CloudBPMNSignalReceivedEvent event1 = new CloudBPMNSignalReceivedEventImpl(
            "id",
            new Date().getTime(),
            new BPMNSignalImpl("elementId"),
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };
        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);

        var messages = List.of(
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "eventType",
                "SIGNAL_RECEIVED"
            )
        );
        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(sendEvents(event1))
            .expectNext(messages)
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionShouldFilterEmptyResults() {
        Map<String, Object> variables = new StringObjectMapBuilder()
            .put("appName", "default-app")
            .put("eventType", "PROCESS_STARTED")
            .get();

        var document =
            """
            subscription($appName: String!, $eventType: EngineEventType!) {
              engineEvents(appName: [$appName], eventType: [$eventType]) {
                processInstanceId
                processDefinitionId
                eventType
              }
            }""";

        CloudBPMNSignalReceivedEvent event1 = new CloudBPMNSignalReceivedEventImpl(
            "id",
            new Date().getTime(),
            new BPMNSignalImpl("elementId"),
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);
        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(sendEvents(event1))
            .expectNoEvent(Duration.ofSeconds(2))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionCloudBPMNTimerEvents() {
        Map<String, Object> variables = new StringObjectMapBuilder()
            .put("appName", "default-app")
            .put(
                "eventTypes",
                Arrays.array(
                    "TIMER_SCHEDULED",
                    "TIMER_FIRED",
                    "TIMER_EXECUTED",
                    "TIMER_CANCELLED",
                    "TIMER_FAILED",
                    "TIMER_RETRIES_DECREMENTED"
                )
            )
            .get();

        var document =
            """
            subscription($appName: String!, $eventTypes: [EngineEventType!]) {
              engineEvents(appName: [$appName], eventType: $eventTypes) {
                processInstanceId
                processDefinitionId
                entity
                eventType
              }
            }""";

        var bpmnTimer = new BPMNTimerImpl("timerId");
        bpmnTimer.setTimerPayload(new TimerPayload());
        CloudBPMNTimerScheduledEvent event1 = new CloudBPMNTimerScheduledEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNTimerFiredEvent event2 = new CloudBPMNTimerFiredEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNTimerExecutedEvent event3 = new CloudBPMNTimerExecutedEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNTimerCancelledEvent event4 = new CloudBPMNTimerCancelledEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNTimerFailedEvent event5 = new CloudBPMNTimerFailedEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNTimerRetriesDecrementedEvent event6 = new CloudBPMNTimerRetriesDecrementedEventImpl(
            "id",
            new Date().getTime(),
            bpmnTimer,
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        var messages = List.of(
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_SCHEDULED"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_FIRED"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_EXECUTED"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_CANCELLED"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_FAILED"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                bpmnTimer,
                "eventType",
                "TIMER_RETRIES_DECREMENTED"
            )
        );

        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);
        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(300))
            .then(sendEvents(event1, event2, event3, event4, event5, event6))
            .expectNextMatches(messageMatches(messages))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionCloudBPMNMessageEvents() {
        Map<String, Object> variables = new StringObjectMapBuilder()
            .put("appName", "default-app")
            .put("eventTypes", Arrays.array("MESSAGE_SENT", "MESSAGE_WAITING", "MESSAGE_RECEIVED"))
            .get();

        var document =
            """
            subscription($appName: String!, $eventTypes: [EngineEventType!]) {
              engineEvents(appName: [$appName], eventType: $eventTypes) {
                processInstanceId
                processDefinitionId
                entity
                eventType
              }
            }""";

        CloudBPMNMessageEvent event1 = new CloudBPMNMessageSentEventImpl(
            "id",
            new Date().getTime(),
            new BPMNMessageImpl("messageId"),
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNMessageEvent event2 = new CloudBPMNMessageWaitingEventImpl(
            "id",
            new Date().getTime(),
            new BPMNMessageImpl("messageId"),
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        CloudBPMNMessageEvent event3 = new CloudBPMNMessageReceivedEventImpl(
            "id",
            new Date().getTime(),
            new BPMNMessageImpl("messageId"),
            "processDefinitionId",
            "processInstanceId"
        ) {
            {
                setAppName("default-app");
                setServiceName("rb-my-app");
                setServiceFullName("serviceFullName");
                setServiceType("runtime-bundle");
                setServiceVersion("");
                setProcessDefinitionId("processDefinitionId");
                setProcessDefinitionKey("processDefinitionKey");
                setProcessDefinitionVersion(1);
                setBusinessKey("businessKey");
            }
        };

        var messages = List.of(
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                new BPMNTimerImpl("messageId"),
                "eventType",
                "MESSAGE_SENT"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                new BPMNTimerImpl("messageId"),
                "eventType",
                "MESSAGE_WAITING"
            ),
            Map.of(
                "processInstanceId",
                "processInstanceId",
                "processDefinitionId",
                "processDefinitionId",
                "entity",
                new BPMNTimerImpl("messageId"),
                "eventType",
                "MESSAGE_RECEIVED"
            )
        );

        Flux<List> flux =
            this.graphQlTester.document(document)
                .variables(variables)
                .executeSubscription()
                .toFlux("engineEvents", List.class);
        StepVerifier
            .create(flux)
            .expectSubscription()
            .thenAwait(Duration.ofMillis(1000))
            .then(sendEvents(event1, event2, event3))
            .expectNextMatches(messageMatches(messages))
            .thenCancel()
            .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlWsSubprotocolServerUnauthorized() throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        var initMessage = objectMapper.writeValueAsString(GraphQlWebSocketMessage.connectionInit(null));

        HttpClient
            .create()
            .baseUrl("ws://localhost:" + port)
            .wiretap(true)
            .websocket(graphqlWsClientSpec)
            .uri(WS_GRAPHQL_URI)
            .handle((i, o) -> {
                o.sendString(Mono.just(initMessage)).then().log("client-send").subscribe();

                return i
                    .aggregateFrames()
                    .receive()
                    .asString()
                    .doOnCancel(() -> {
                        closeWebSocketAnCompleteDataProcessor(output, o);
                    });
            })
            .log("client-received")
            .take(1)
            .subscribeWith(output)
            .collectList()
            .subscribe();

        StepVerifier.create(output).expectComplete().verify(TIMEOUT);
    }

    @Test
    public void testGraphql() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
            "{Tasks(where:{name:{EQ: \"" + TASK_NAME + "\"}}){select{id assignee priority}}}"
        );

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        assertThat("{Tasks={select=[{id=1, assignee=assignee, priority=5}]}}").isEqualTo(result.getData().toString());
    }

    @Test
    public void testGraphqlUnauthorized() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
            "{Tasks(where:{name:{EQ: \"" + TASK_NAME + "\"}}){select{id assignee priority}}}"
        );

        identityTokenProducer.withTestUser(HRUSER);
        authHeaders = identityTokenProducer.authorizationHeaders();

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(HttpStatus.FORBIDDEN).describedAs(entity.toString()).isEqualTo(entity.getStatusCode());
    }

    @Test
    public void testGraphqlTasksQueryWithEQNullValues() {
        // @formatter:off
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                "query {" +
                "  Tasks(" +
                "    page: { start: 1, limit: 100 }" +
                "    where: { status: { IN: [CREATED, ASSIGNED] }, dueDate: { EQ: null } }" +
                "  ) {" +
                "    select {" +
                "      id" +
                "      businessKey" +
                "      name" +
                "      status" +
                "      priority(orderBy: DESC)" +
                "      dueDate(orderBy: ASC)" +
                "      assignee" +
                "    }" +
                "  }" +
                "}");
        // @formatter:on

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        String expected =
            "{Tasks={select=[" +
            "{id=2, businessKey=null, name=task2, status=CREATED, priority=10, dueDate=null, assignee=assignee}, " +
            "{id=4, businessKey=null, name=task4, status=CREATED, priority=10, dueDate=null, assignee=assignee}, " +
            "{id=6, businessKey=bk6, name=task6, status=ASSIGNED, priority=10, dueDate=null, assignee=assignee}, " +
            "{id=3, businessKey=null, name=task3, status=CREATED, priority=5, dueDate=null, assignee=assignee}" +
            "]}}";

        assertThat(result.getData().toString()).isEqualTo(expected);
    }

    @Test
    public void testGraphqlTasksQueryWithNENullValues() {
        // @formatter:off
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                "query {" +
                    "  Tasks(" +
                    "    page: { start: 1, limit: 100 }" +
                    "    where: { status: { IN: [ASSIGNED, COMPLETED] }, businessKey: { NE: null } }" +
                    "  ) {" +
                    "    select {" +
                    "      id" +
                    "      businessKey" +
                    "      name" +
                    "      status" +
                    "      priority(orderBy: DESC)" +
                    "      dueDate(orderBy: ASC)" +
                    "      assignee" +
                    "    }" +
                    "  }" +
                    "}");
        // @formatter:on

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        String expected =
            "{Tasks={select=[" +
            "{id=6, businessKey=bk6, name=task6, status=ASSIGNED, priority=10, dueDate=null, assignee=assignee}, " +
            "{id=1, businessKey=bk1, name=task1, status=COMPLETED, priority=5, dueDate=null, assignee=assignee}" +
            "]}}";

        assertThat(result.getData().toString()).isEqualTo(expected);
    }

    @Test
    public void testGraphqlWhere() {
        // @formatter:off
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                    "query {" +
                    "	  ProcessInstances(page: {start: 1, limit: 10}," +
                    "	    where: {status : {EQ: COMPLETED }}) {" +
                    "	    pages" +
                    "	    total" +
                    "	    select {" +
                    "	      id" +
                    "	      processDefinitionId" +
                    "	      processDefinitionKey" +
                    "	      status" +
                    "	      tasks {" +
                    "	        name" +
                    "	        status" +
                    "	      }" +
                    "	    }" +
                    "	  }" +
                    "	}");
        // @formatter:on

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        assertThat(((Map<String, Object>) result.getData()).get("ProcessInstances")).isNotNull();
    }

    @Test
    public void testGraphqlNesting() {
        // @formatter:off
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                    "query {"
                    + "ProcessInstances {"
                    + "    select {"
                    + "      id"
                    + "      tasks {"
                    + "        id"
                    + "        name"
                    + "        variables {"
                    + "          name"
                    + "          value"
                    + "        }"
                    + "        taskCandidateUsers {"
                    + "           taskId"
                    + "           userId"
                    + "        }"
                    + "        taskCandidateGroups {"
                    + "           taskId"
                    + "           groupId"
                    + "        }"
                    + "      }"
                    + "      variables {"
                    + "        name"
                    + "        value"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}");
        // @formatter:on

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        assertThat(((Map<String, Object>) result.getData()).get("ProcessInstances")).isNotNull();
    }

    @Test
    public void testGraphqlReverse() {
        // @formatter:off
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                    " query {"
                    + " ProcessVariables {"
                    + "    select {"
                    + "      id"
                    + "      name"
                    + "      value"
                    + "      processInstance(where: {status: {EQ: RUNNING}}) {"
                    + "        id"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}"
            );
        // @formatter:on

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        assertThat(result.getData().get("ProcessVariables")).isNotNull();
    }

    @Test
    public void testGraphqlArguments() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
            "query TasksQuery($name: String!) {Tasks(where:{name:{EQ: $name}}) {select{id assignee priority}}}"
        );

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("name", TASK_NAME);

        query.setVariables(variables);

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        assertThat("{Tasks={select=[{id=1, assignee=assignee, priority=5}]}}").isEqualTo(result.getData().toString());
    }

    @Test
    public void testGraphqlAggregateTaskVariablesQuery() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
            """
                query {
                  TaskVariables(
                    # Apply filter criteria
                    where: {name: {IN: ["variable1", "variable2", "variable3"]}}
                  ) {
                    aggregate {
                      # count by variables
                      variables: count
                      # Count by associated tasks
                      groupByVariableName: group {
                        name: by(field: name)
                        count
                      }
                      by {
                        groupByTaskStatus: task {
                          status: by(field: status)
                          count
                        }
                        # Count by associated tasks
                        groupByTaskAssignee: task {
                          assignee: by(field: assignee)
                          count
                        }
                      }
                    }
                  }
                }
            """
        );

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        var expected =
            "{TaskVariables={aggregate={variables=3, groupByVariableName=[{name=variable1, count=1}, {name=variable2, count=1}, {name=variable3, count=1}], by={groupByTaskStatus=[{status=COMPLETED, count=2}, {status=CREATED, count=1}], groupByTaskAssignee=[{assignee=assignee, count=3}]}}}}";

        assertThat(result.getData().toString()).isEqualTo(expected);
    }

    @Test
    public void testGraphqlAggregateTasksQuery() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
            """
                query {
                  Tasks {
                    aggregate {
                      countTasks: count
                      countProcessVariables: count(of: processVariables)
                      countTaskVariables: count(of: variables)
                      countTasksGroupedByStatus: group {
                        status: by(field: status)
                        count
                      }
                      countProcessVariablesGroupedByTaskName: group {
                        name: by(field: name)
                        count(of: processVariables)
                      }
                      by {
                        countTaskProcessVariablesGroupedByVariableNameAndValue: processVariables {
                          name: by(field: name)
                          value: by(field: value)
                          count
                        }
                        countTaskVariablesGroupedByVariableName: variables {
                          name: by(field: name)
                          count
                        }
                      }
                    }
                  }
                }
            """
        );

        ResponseEntity<GraphQLQueryResult> entity = rest.postForEntity(
            GRAPHQL_URL,
            new HttpEntity<>(query, authHeaders),
            GraphQLQueryResult.class
        );

        assertThat(entity.getStatusCode()).describedAs(entity.toString()).isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNull();

        var expected =
            "{Tasks={aggregate={countTasks=6, countProcessVariables=2, countTaskVariables=6, countTasksGroupedByStatus=[{status=ASSIGNED, count=1}, {status=COMPLETED, count=2}, {status=CREATED, count=3}], countProcessVariablesGroupedByTaskName=[{name=task4, count=1}, {name=task5, count=1}], by={countTaskProcessVariablesGroupedByVariableNameAndValue=[{name=initiator, value={key=[1, 2, 3, 4, 5]}, count=2}], countTaskVariablesGroupedByVariableName=[{name=variable1, count=1}, {name=variable2, count=1}, {name=variable3, count=1}, {name=variable4, count=1}, {name=variable5, count=1}, {name=variable6, count=1}]}}}}";

        assertThat(result.getData().toString()).isEqualTo(expected);
    }

    private static Predicate<List> messageMatches(List<Map<String, Object>> messages) {
        return m ->
            messages
                .stream()
                .allMatch(message ->
                    m
                        .stream()
                        .anyMatch(o -> {
                            var map = ((Map) o);
                            return (
                                map.get("processInstanceId").equals(message.get("processInstanceId")) &&
                                map.get("processDefinitionId").equals(message.get("processDefinitionId")) &&
                                map.get("eventType").equals(message.get("eventType"))
                            );
                        })
                );
    }

    public static StringObjectMapBuilder mapBuilder() {
        return new StringObjectMapBuilder();
    }
}
