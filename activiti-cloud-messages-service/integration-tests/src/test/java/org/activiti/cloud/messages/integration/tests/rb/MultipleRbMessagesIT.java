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
package org.activiti.cloud.messages.integration.tests.rb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.activiti.api.model.shared.Payload;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.services.core.commands.CommandEndpoint;
import org.activiti.cloud.services.core.commands.ReceiveMessageCmdExecutor;
import org.activiti.cloud.services.core.commands.StartMessageCmdExecutor;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageReceivedEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageSentEventMessageProducer;
import org.activiti.cloud.services.messages.events.producer.BpmnMessageWaitingEventMessageProducer;
import org.activiti.cloud.services.messages.events.support.BpmnMessageEventMessageBuilderFactory;
import org.activiti.cloud.services.messages.events.support.MessageEventsDispatcher;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

@Testcontainers
class MultipleRbMessagesIT {
    private static final String INTERMEDIATE_CATCH_MESSAGE_PROCESS = "IntermediateCatchMessageProcess";
    private static final String INTERMEDIATE_THROW_MESSAGE_PROCESS = "IntermediateThrowMessageProcess";
    private static final String BUSINESS_KEY = "businessKey";

    private static ConfigurableApplicationContext h2Ctx;
    private static ConfigurableApplicationContext rbCtx1;
    private static ConfigurableApplicationContext rbCtx2;

    @Container
    private static GenericContainer keycloakContainer =
        new GenericContainer("activiti/activiti-keycloak")
            .withExposedPorts(8180)
            .waitingFor(Wait.defaultWaitStrategy());

    @Container
    private static RabbitMQContainer rabbitMQContainer =
        new RabbitMQContainer("rabbitmq:3.8.9-management-alpine");

    @Configuration
    @Profile("h2")
    static class H2Application {

        @Bean(initMethod = "start", destroyMethod = "stop")
        public Server inMemoryH2DatabaseaServer() throws SQLException {
            return Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9090");
        }
    }

    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {

        @Bean
        public BpmnMessageReceivedEventMessageProducer throwMessageReceivedEventListener(MessageEventsDispatcher messageEventsDispatcher,
                                                                                         BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
            return spy(new BpmnMessageReceivedEventMessageProducer(messageEventsDispatcher,
                                                                   messageBuilderFactory));
        }

        @Bean
        public BpmnMessageWaitingEventMessageProducer throwMessageWaitingEventMessageProducer(MessageEventsDispatcher messageEventsDispatcher,
                                                                                              BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
            return spy(new BpmnMessageWaitingEventMessageProducer(messageEventsDispatcher,
                                                                  messageBuilderFactory));
        }

        @Bean
        public BpmnMessageSentEventMessageProducer bpmnMessageSentEventProducer(MessageEventsDispatcher messageEventsDispatcher,
                                                                                BpmnMessageEventMessageBuilderFactory messageBuilderFactory) {
            return spy(new BpmnMessageSentEventMessageProducer(messageEventsDispatcher,
                                                               messageBuilderFactory));
        }

        @Bean
        public StartMessageCmdExecutor startMessageCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
            return spy(new StartMessageCmdExecutor(processAdminRuntime));
        }

        @Bean
        public ReceiveMessageCmdExecutor receiveMessageCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
            return spy(new ReceiveMessageCmdExecutor(processAdminRuntime));
        }

    }

    @BeforeAll
    public static void setUp() {
        System.setProperty("keycloak.auth-server-url", "http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getFirstMappedPort() + "/auth");

        System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getContainerIpAddress());
        System.setProperty("spring.rabbitmq.port", String.valueOf(rabbitMQContainer.getAmqpPort()));

        h2Ctx = new SpringApplicationBuilder(H2Application.class).web(WebApplicationType.NONE)
                                                                 .profiles("h2")
                                                                 .run();

        rbCtx1 = new SpringApplicationBuilder(RbApplication.class).properties("server.port=8081",
                                                                              "spring.application.name=rb1")
                                                                  .run();

        rbCtx2 = new SpringApplicationBuilder(RbApplication.class).properties("server.port=8082",
                                                                              "spring.application.name=rb2")
                                                                  .run();

    }

    @AfterAll
    public static void tearDown() {
        rbCtx1.close();
        rbCtx2.close();
        h2Ctx.close();
    }

    @Test
    void contextLoads() throws Exception {
        assertThat(h2Ctx).isNotNull();
        assertThat(rbCtx1).isNotNull();
        assertThat(rbCtx2).isNotNull();
    }

    @Test
    void shouldHandleBpmnMessagesBetweenMulitpleRuntimeBundles() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .build();

        StartProcessPayload catchProcessPayload = ProcessPayloadBuilder.start()
                                                                       .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
                                                                       .withBusinessKey(BUSINESS_KEY)
                                                                       .build();

        //when
        executeCommand(rbCtx1, throwProcessPayload);
        executeCommand(rbCtx2, throwProcessPayload);
        executeCommand(rbCtx1, catchProcessPayload);
        executeCommand(rbCtx2, catchProcessPayload);

        //then
        assertThrowCatchBpmnMessages(rbCtx1);
        assertThrowCatchBpmnMessages(rbCtx2);
    }

    void executeCommand(ConfigurableApplicationContext context,
                        Payload payload) {
        CommandEndpoint<Payload> commandEndpoint = context.getBean(CommandEndpoint.class);
        commandEndpoint.execute(payload);
    }

    void assertThrowCatchBpmnMessages(ConfigurableApplicationContext context) {
        BpmnMessageReceivedEventMessageProducer bpmnMessageReceivedEventMessageProducer = context.getBean(BpmnMessageReceivedEventMessageProducer.class);
        BpmnMessageSentEventMessageProducer bpmnMessageSentEventMessageProducer = context.getBean(BpmnMessageSentEventMessageProducer.class);
        BpmnMessageWaitingEventMessageProducer bpmnMessageWaitingEventMessageProducer = context.getBean(BpmnMessageWaitingEventMessageProducer.class);
        StartMessageCmdExecutor startMessageCmdExecutor = context.getBean(StartMessageCmdExecutor.class);
        ReceiveMessageCmdExecutor receiveMessageCmdExecutor = context.getBean(ReceiveMessageCmdExecutor.class);

        await().untilAsserted(() -> {
            verify(bpmnMessageSentEventMessageProducer, times(1)).onEvent(any());
            verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(any());
            verify(bpmnMessageReceivedEventMessageProducer, times(1)).onEvent(any());

            verify(receiveMessageCmdExecutor, times(1)).execute(any());
            verify(startMessageCmdExecutor, never()).execute(any());
        });
    }
}
