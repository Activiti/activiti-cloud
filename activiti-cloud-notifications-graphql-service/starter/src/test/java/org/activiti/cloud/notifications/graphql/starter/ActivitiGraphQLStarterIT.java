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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.introproventures.graphql.jpa.query.web.GraphQLController.GraphQLQueryRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
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
import org.activiti.cloud.notifications.graphql.test.EngineEventsMessageProducer;
import org.activiti.cloud.notifications.graphql.test.EngineEventsMessageProducer.EngineEvents;
import org.activiti.cloud.services.notifications.graphql.web.api.GraphQLQueryResult;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessage;
import org.activiti.cloud.services.notifications.graphql.ws.api.GraphQLMessageType;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.apache.groovy.util.Maps;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.WebsocketSender;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ActivitiGraphQLStarterIT {

    private static final String WS_GRAPHQL_URI = "/ws/graphql";
    private static final String GRAPHQL_WS = "graphql-ws";
    private static final String HRUSER = "hruser";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TESTADMIN = "testadmin";
    private static final String TASK_NAME = "task1";
    private static final String GRAPHQL_URL = "/graphql";
    private static final Duration TIMEOUT = Duration.ofMillis(20000);


    @LocalServerPort
    private String port;

    @Autowired
    private KeycloakTokenProducer keycloakTokenProducer;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private EngineEvents producerChannel;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpHeaders authHeaders;

    @SpringBootApplication
    @EnableBinding(EngineEventsMessageProducer.EngineEvents.class)
    static class Application {
        // Nothing
    }

    @BeforeEach
    public void setUp() {

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        authHeaders = keycloakTokenProducer.authorizationHeaders();
        authHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    protected URI getUrl(String path) throws URISyntaxException {
        return new URI("ws://localhost:" + this.port + path);
    }

    @Test
    public void testGraphqlWsSubprotocolConnectionInitXAuthorizationSupported() throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String accessToken = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> payload = new StringObjectMapBuilder().put("kaInterval", 1000)
                                              .put("X-Authorization", accessToken)
                                              .get();

        String initMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.CONNECTION_INIT)
                                                                     .payload(payload)
                                                                     .build());

        HttpClient.create()
                .baseUrl("ws://localhost:" + port)
                .wiretap(true)
                .websocket(GRAPHQL_WS)
                .uri(WS_GRAPHQL_URI)
                .handle((i, o) -> {
                    o.sendString(Mono.just(initMessage))
                            .then()
                            .log("client-send")
                            .subscribe();

                    return i.receive().asString();
                })
                .log("client-received")
                .take(2)
                .subscribeWith(output)
                .collectList()
                .subscribe();

        String ackMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                    .type(GraphQLMessageType.CONNECTION_ACK)
                                                                    .build());

        String kaMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                   .type(GraphQLMessageType.KA)
                                                                   .build());

        StepVerifier.create(output)
                .expectNext(ackMessage)
                .expectNext(kaMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }


    @Test
    public void testGraphqlWsSubprotocolServerStartStopSubscription() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = mapBuilder().put("appName", "default-app")
                                                .put("eventTypes", Arrays.array("PROCESS_CREATED", "PROCESS_STARTED"))
                                                .get();

        Map<String, Object> payload = mapBuilder().put("query", "subscription($appName: String!, $eventTypes: [EngineEventType!]) { "
                                                                + "  engineEvents(appName: [$appName], eventType: $eventTypes) { "
                                                                + "    processInstanceId  "
                                                                + "    eventType "
                                                                + "  } "
                                                                + "}")
                                              .put("variables", variables)
                                              .get();

        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .take(1)
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder.withPayload(Arrays.array(event1, event2))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        // then
        Map<String, Object> message = Maps.of("data",
                                              Maps.of("engineEvents",
                                                      Arrays.array(Maps.of("processInstanceId", "processInstanceId",
                                                                           "eventType", "PROCESS_CREATED"),
                                                                   Maps.of("processInstanceId", "processInstanceId",
                                                                           "eventType", "PROCESS_STARTED")
                                                                  )
                                                     )
                                             );

        String dataMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.DATA)
                                                                     .id("1")
                                                                     .payload(message)
                                                                     .build());
        StepVerifier.create(data)
                .expectNext(dataMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionPROCESS_DEPLOYED() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app")
                                                .get();

        Map<String, Object> payload = new StringObjectMapBuilder().put("query", "subscription($appName: String!) { "
                                                                                + "  engineEvents(appName: [$appName], eventType: PROCESS_DEPLOYED) { "
                                                                                + "    processDefinitionKey "
                                                                                + "    eventType "
                                                                                + "  } "
                                                                                + "}")
                                              .put("variables", variables)
                                              .get();
        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
        CloudProcessDeployedEvent event1 = new CloudProcessDeployedEventImpl("id", new Date().getTime(), new ProcessDefinitionEntity()) {
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .take(1)
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder.withPayload(Arrays.array(event1))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        // then
        Map<String, Object> message = Maps.of("data",
                                              Maps.of("engineEvents", Arrays.array(mapBuilder().put("processDefinitionKey", "processDefinitionKey")
                                                                                           .put("eventType", "PROCESS_DEPLOYED")
                                                                                           .get()))
                                             );

        String dataMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.DATA)
                                                                     .id("1")
                                                                     .payload(message)
                                                                     .build());
        StepVerifier.create(data)
                .expectNext(dataMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }


    @Test
    public void testGraphqlSubscriptionSIGNAL_RECEIVED() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app")
                                                .put("eventType", "SIGNAL_RECEIVED")
                                                .get();

        Map<String, Object> payload = new StringObjectMapBuilder().put("query", "subscription($appName: String!, $eventType: EngineEventType!) { "
                                                                                + "  engineEvents(appName: [$appName], eventType: [$eventType]) { "
                                                                                + "    processInstanceId "
                                                                                + "    processDefinitionId "
                                                                                + "    eventType "
                                                                                + "  } "
                                                                                + "}")
                                              .put("variables", variables)
                                              .get();
        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
        CloudBPMNSignalReceivedEvent event1 = new CloudBPMNSignalReceivedEventImpl("id",
                                                                                   new Date().getTime(),
                                                                                   new BPMNSignalImpl("elementId"),
                                                                                   "processDefinitionId",
                                                                                   "processInstanceId") {
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .take(1)
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder.withPayload(Arrays.array(event1))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        // then
        Map<String, Object> message = Maps.of("data",
                                              Maps.of("engineEvents",
                                                      Arrays.array(mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("eventType", "SIGNAL_RECEIVED")
                                                                           .get()
                                                                  )
                                                     )
                                             );

        String dataMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.DATA)
                                                                     .id("1")
                                                                     .payload(message)
                                                                     .build());
        StepVerifier.create(data)
                .expectNext(dataMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionShouldFilterEmptyResults() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app")
                                                .put("eventType", "PROCESS_STARTED")
                                                .get();

        Map<String, Object> payload = new StringObjectMapBuilder()
                                              .put("query", "subscription($appName: String!, $eventType: EngineEventType!) { "
                                                            + "  engineEvents(appName: [$appName], eventType: [$eventType]) { "
                                                            + "    processInstanceId "
                                                            + "    processDefinitionId "
                                                            + "    eventType "
                                                            + "  } "
                                                            + "}")
                                              .put("variables", variables)
                                              .get();
        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
        CloudBPMNSignalReceivedEvent event1 = new CloudBPMNSignalReceivedEventImpl("id",
                                                                                   new Date().getTime(),
                                                                                   new BPMNSignalImpl("elementId"),
                                                                                   "processDefinitionId",
                                                                                   "processInstanceId") {
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .timeout(Duration.ofSeconds(2))
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder.withPayload(Arrays.array(event1))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        StepVerifier.create(data)
                .expectSubscription()
                .expectError(TimeoutException.class)
                .verify();
    }


    @Test
    public void testGraphqlSubscriptionCloudBPMNTimerEvents() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app")
                                                .put("eventTypes", Arrays.array("TIMER_SCHEDULED",
                                                                                "TIMER_FIRED",
                                                                                "TIMER_EXECUTED",
                                                                                "TIMER_CANCELLED",
                                                                                "TIMER_FAILED",
                                                                                "TIMER_RETRIES_DECREMENTED"))
                                                .get();

        Map<String, Object> payload = new StringObjectMapBuilder()
                                              .put("query", "subscription($appName: String!, $eventTypes: [EngineEventType!]) { "
                                                            + "  engineEvents(appName: [$appName], eventType: $eventTypes) { "
                                                            + "    processInstanceId "
                                                            + "    processDefinitionId "
                                                            + "    entity "
                                                            + "    eventType "
                                                            + "  } "
                                                            + "}")
                                              .put("variables", variables)
                                              .get();
        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
        CloudBPMNTimerScheduledEvent event1 = new CloudBPMNTimerScheduledEventImpl("id",
                                                                                   new Date().getTime(),
                                                                                   new BPMNTimerImpl("timerId"),
                                                                                   "processDefinitionId",
                                                                                   "processInstanceId") {
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

        // given
        CloudBPMNTimerFiredEvent event2 = new CloudBPMNTimerFiredEventImpl("id",
                                                                           new Date().getTime(),
                                                                           new BPMNTimerImpl("timerId"),
                                                                           "processDefinitionId",
                                                                           "processInstanceId") {
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

        // given
        CloudBPMNTimerExecutedEvent event3 = new CloudBPMNTimerExecutedEventImpl("id",
                                                                                 new Date().getTime(),
                                                                                 new BPMNTimerImpl("timerId"),
                                                                                 "processDefinitionId",
                                                                                 "processInstanceId") {
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

        // given
        CloudBPMNTimerCancelledEvent event4 = new CloudBPMNTimerCancelledEventImpl("id",
                                                                                   new Date().getTime(),
                                                                                   new BPMNTimerImpl("timerId"),
                                                                                   "processDefinitionId",
                                                                                   "processInstanceId") {
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

        // given
        CloudBPMNTimerFailedEvent event5 = new CloudBPMNTimerFailedEventImpl("id",
                                                                             new Date().getTime(),
                                                                             new BPMNTimerImpl("timerId"),
                                                                             "processDefinitionId",
                                                                             "processInstanceId") {
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

        // given
        CloudBPMNTimerRetriesDecrementedEvent event6 = new CloudBPMNTimerRetriesDecrementedEventImpl(
                "id",
                new Date().getTime(),
                new BPMNTimerImpl("timerId"),
                "processDefinitionId",
                "processInstanceId") {
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .take(1)
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder
                                                                     .withPayload(Arrays.array(event1, event2, event3, event4, event5, event6))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        // then
        Map<String, Object> message = Maps.of("data",
                                              Maps.of("engineEvents",
                                                      Arrays.array(mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_SCHEDULED")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_FIRED")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_EXECUTED")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_CANCELLED")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_FAILED")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("timerId"))
                                                                           .put("eventType", "TIMER_RETRIES_DECREMENTED")
                                                                           .get()
                                                                  )
                                                     )
                                             );

        String dataMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.DATA)
                                                                     .id("1")
                                                                     .payload(message)
                                                                     .build());
        StepVerifier.create(data)
                .expectNext(dataMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }

    @Test
    public void testGraphqlSubscriptionCloudBPMNMessageEvents() throws JsonProcessingException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(TESTADMIN);
        final String auth = keycloakTokenProducer.authorizationHeaders().getFirst(AUTHORIZATION);

        Map<String, Object> variables = new StringObjectMapBuilder().put("appName", "default-app")
                                                .put("eventTypes", Arrays.array("MESSAGE_SENT",
                                                                                "MESSAGE_WAITING",
                                                                                "MESSAGE_RECEIVED"))
                                                .get();

        Map<String, Object> payload = new StringObjectMapBuilder()
                                              .put("query", "subscription($appName: String!, $eventTypes: [EngineEventType!]) { "
                                                            + "  engineEvents(appName: [$appName], eventType: $eventTypes) { "
                                                            + "    processInstanceId "
                                                            + "    processDefinitionId "
                                                            + "    entity "
                                                            + "    eventType "
                                                            + "  } "
                                                            + "}")
                                              .put("variables", variables)
                                              .get();
        GraphQLMessage start = GraphQLMessage.builder()
                                       .type(GraphQLMessageType.START)
                                       .id("1")
                                       .payload(payload)
                                       .build();

        String startMessage = objectMapper.writeValueAsString(start);

        // given
        CloudBPMNMessageEvent event1 = new CloudBPMNMessageSentEventImpl("id",
                                                                         new Date().getTime(),
                                                                         new BPMNMessageImpl("messageId"),
                                                                         "processDefinitionId",
                                                                         "processInstanceId") {
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

        // given
        CloudBPMNMessageEvent event2 = new CloudBPMNMessageWaitingEventImpl("id",
                                                                            new Date().getTime(),
                                                                            new BPMNMessageImpl("messageId"),
                                                                            "processDefinitionId",
                                                                            "processInstanceId") {
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

        // given
        CloudBPMNMessageEvent event3 = new CloudBPMNMessageReceivedEventImpl("id",
                                                                             new Date().getTime(),
                                                                             new BPMNMessageImpl("messageId"),
                                                                             "processDefinitionId",
                                                                             "processInstanceId") {
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

        WebsocketSender client = HttpClient.create()
                                         .baseUrl("ws://localhost:" + port)
                                         .wiretap(true)
                                         .headers(h -> h.add(AUTHORIZATION, auth))
                                         .websocket(GRAPHQL_WS)
                                         .uri(WS_GRAPHQL_URI);

        // start subscription
        client.handle((i, o) -> {
            o.sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                           .asString()
                           .log("data")
                           .take(1)
                           .doOnSubscribe(s -> producerChannel.output()
                                                       .send(MessageBuilder.withPayload(Arrays.array(event1, event2, event3))
                                                                     .setHeader("routingKey", "eventProducer")
                                                                     .build()))
                           .delaySubscription(Duration.ofSeconds(1))
                           .subscribeWith(data);
        }) // stop subscription
                .collectList()
                .subscribe();

        // then
        Map<String, Object> message = Maps.of("data",
                                              Maps.of("engineEvents",
                                                      Arrays.array(mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("messageId"))
                                                                           .put("eventType", "MESSAGE_SENT")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("messageId"))
                                                                           .put("eventType", "MESSAGE_WAITING")
                                                                           .get(),
                                                                   mapBuilder().put("processInstanceId", "processInstanceId")
                                                                           .put("processDefinitionId", "processDefinitionId")
                                                                           .put("entity", new BPMNTimerImpl("messageId"))
                                                                           .put("eventType", "MESSAGE_RECEIVED")
                                                                           .get()
                                                                  )
                                                     )
                                             );

        String dataMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.DATA)
                                                                     .id("1")
                                                                     .payload(message)
                                                                     .build());
        StepVerifier.create(data)
                .expectNext(dataMessage)
                .expectComplete()
                .verify(TIMEOUT);
    }


    @Test
    public void testGraphqlWsSubprotocolServerWithUserRoleNotAuthorized()
            throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        keycloakTokenProducer.setKeycloakTestUser(HRUSER);

        final String accessToken = keycloakTokenProducer.authorizationHeaders()
                                           .getFirst(AUTHORIZATION);

        Map<String, Object> payload = mapBuilder().put("X-Authorization", accessToken)
                                              .get();

        String initMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.CONNECTION_INIT)
                                                                     .payload(payload)
                                                                     .build());
        HttpClient.create()
                .baseUrl("ws://localhost:" + port)
                .wiretap(true)
                .websocket(GRAPHQL_WS)
                .uri(WS_GRAPHQL_URI)
                .handle((i, o) -> {
                    o.sendString(Mono.just(initMessage))
                            .then()
                            .log("client-send")
                            .subscribe();

                    return i.receive().asString();
                })
                .log("client-received")
                .take(1)
                .subscribeWith(output)
                .collectList()
                .doOnError(i -> System.err.println("Failed requesting server: " + i))
                .subscribe();

        String expected = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                  .type(GraphQLMessageType.CONNECTION_ERROR)
                                                                  .build());
        StepVerifier.create(output)
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    public void testGraphqlWsSubprotocolServerUnauthorized() throws JsonProcessingException {
        ReplayProcessor<String> output = ReplayProcessor.create();

        String initMessage = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                     .type(GraphQLMessageType.CONNECTION_INIT)
                                                                     .build());
        HttpClient.create()
                .baseUrl("ws://localhost:" + port)
                .wiretap(true)
                //.headers(h -> h.add(AUTHORIZATION, auth)) // Anonymous request
                .websocket(GRAPHQL_WS)
                .uri(WS_GRAPHQL_URI)
                .handle((i, o) -> {
                    o.sendString(Mono.just(initMessage))
                            .then()
                            .log("client-send")
                            .subscribe();

                    return i.receive().asString();
                })
                .log("client-received")
                .take(1)
                .subscribeWith(output)
                .collectList()
                .doOnError(i -> System.err.println("Failed requesting server: " + i))
                .subscribe();

        String expected = objectMapper.writeValueAsString(GraphQLMessage.builder()
                                                                  .type(GraphQLMessageType.CONNECTION_ERROR)
                                                                  .build());
        StepVerifier.create(output)
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    public void testGraphql() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                "{Tasks(where:{name:{EQ: \"" + TASK_NAME + "\"}}){select{id assignee priority}}}");

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(entity.getStatusCode())
                .describedAs(entity.toString())
                .isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors())
                .isNull();

        assertThat("{Tasks={select=[{id=1, assignee=assignee, priority=5}]}}")
                .isEqualTo(result.getData().toString());

    }

    @Test
    public void testGraphqlUnauthorized() {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                "{Tasks(where:{name:{EQ: \"" + TASK_NAME + "\"}}){select{id assignee priority}}}");

        keycloakTokenProducer.setKeycloakTestUser(HRUSER);
        authHeaders = keycloakTokenProducer.authorizationHeaders();

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(HttpStatus.FORBIDDEN)
                .describedAs(entity.toString())
                .isEqualTo(entity.getStatusCode());

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

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(entity.getStatusCode())
                .describedAs(entity.toString())
                .isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors())
                .isNull();

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

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(entity.getStatusCode())
                .describedAs(entity.toString())
                .isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors())
                .isNull();

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

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(entity.getStatusCode())
                .describedAs(entity.toString())
                .isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors())
                .isNull();

        assertThat(result.getData().get("ProcessVariables")).isNotNull();
    }

    @Test
    public void testGraphqlArguments()
            throws JsonParseException, JsonMappingException, IOException {
        GraphQLQueryRequest query = new GraphQLQueryRequest(
                "query TasksQuery($name: String!) {Tasks(where:{name:{EQ: $name}}) {select{id assignee priority}}}");

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("name", TASK_NAME);

        query.setVariables(variables);

        ResponseEntity<GraphQLQueryResult> entity = rest
                                                            .postForEntity(GRAPHQL_URL, new HttpEntity<>(query, authHeaders),
                                                                           GraphQLQueryResult.class);

        assertThat(entity.getStatusCode())
                .describedAs(entity.toString())
                .isEqualTo(HttpStatus.OK);

        GraphQLQueryResult result = entity.getBody();

        assertThat(result).isNotNull();
        assertThat(result.getErrors())
                .isNull();

        assertThat("{Tasks={select=[{id=1, assignee=assignee, priority=5}]}}")
                .isEqualTo(result.getData().toString());
    }

    public static StringObjectMapBuilder mapBuilder() {
        return new StringObjectMapBuilder();
    }

}


/**
 * A map builder creating a map with String keys and values.
 */
class StringObjectMapBuilder extends MapBuilder<StringObjectMapBuilder, String, Object> {

}

class MapBuilder<B extends MapBuilder<B, K, V>, K, V> {

    private final Map<K, V> map = new LinkedHashMap<>();

    public B put(K key, V value) {
        this.map.put(key, value);
        return _this();
    }

    public Map<K, V> get() {
        return this.map;
    }

    @SuppressWarnings("unchecked")
    protected final B _this() {
        return (B) this;
    }

}

