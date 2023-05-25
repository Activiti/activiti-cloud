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

import java.sql.SQLException;
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
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class MultipleRbMessagesIT {

    private static final String INTERMEDIATE_CATCH_MESSAGE_PROCESS = "IntermediateCatchMessageProcess";
    private static final String INTERMEDIATE_THROW_MESSAGE_PROCESS = "IntermediateThrowMessageProcess";
    private static final String BUSINESS_KEY = "businessKey";

    private static ConfigurableApplicationContext h2Context;
    private static ConfigurableApplicationContext rb1Context;
    private static ConfigurableApplicationContext rb2Context;

    @Configuration
    @Profile("h2")
    static class H2Application {

        @Bean(initMethod = "start", destroyMethod = "stop")
        public Server inMemoryH2DatabaseServer() throws SQLException {
            return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9090");
        }
    }

    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {

        @Bean
        public BpmnMessageReceivedEventMessageProducer throwMessageReceivedEventListener(
            MessageEventsDispatcher messageEventsDispatcher,
            BpmnMessageEventMessageBuilderFactory messageBuilderFactory
        ) {
            return spy(new BpmnMessageReceivedEventMessageProducer(messageEventsDispatcher, messageBuilderFactory));
        }

        @Bean
        public BpmnMessageWaitingEventMessageProducer throwMessageWaitingEventMessageProducer(
            MessageEventsDispatcher messageEventsDispatcher,
            BpmnMessageEventMessageBuilderFactory messageBuilderFactory
        ) {
            return spy(new BpmnMessageWaitingEventMessageProducer(messageEventsDispatcher, messageBuilderFactory));
        }

        @Bean
        public BpmnMessageSentEventMessageProducer bpmnMessageSentEventProducer(
            MessageEventsDispatcher messageEventsDispatcher,
            BpmnMessageEventMessageBuilderFactory messageBuilderFactory
        ) {
            return spy(new BpmnMessageSentEventMessageProducer(messageEventsDispatcher, messageBuilderFactory));
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
        KeycloakContainerApplicationInitializer keycloakContainerApplicationInitializer = new KeycloakContainerApplicationInitializer();
        keycloakContainerApplicationInitializer.initialize();
        RabbitMQContainerApplicationInitializer rabbitMQContainerApplicationInitializer = new RabbitMQContainerApplicationInitializer();
        rabbitMQContainerApplicationInitializer.initialize();
        TestPropertyValues
            .of(KeycloakContainerApplicationInitializer.getContainerProperties())
            .and(RabbitMQContainerApplicationInitializer.getContainerProperties())
            .applyToSystemProperties(() -> {
                h2Context =
                    new SpringApplicationBuilder(H2Application.class)
                        .web(WebApplicationType.NONE)
                        .properties("spring.main.banner-mode=off")
                        .profiles("h2")
                        .run();

                rb1Context =
                    new SpringApplicationBuilder(RbApplication.class)
                        .properties(
                            "server.port=8081",
                            "spring.main.banner-mode=off",
                            "activiti.cloud.application.name=messages-app1",
                            "spring.application.name=rb"
                        )
                        .run();

                rb2Context =
                    new SpringApplicationBuilder(RbApplication.class)
                        .properties(
                            "server.port=8082",
                            "spring.main.banner-mode=off",
                            "activiti.cloud.application.name=messages-app2",
                            "spring.application.name=rb"
                        )
                        .run();

                return true;
            });
    }

    @AfterAll
    public static void tearDown() {
        rb1Context.close();
        rb2Context.close();
        h2Context.close();
    }

    @Test
    void contextLoads() {
        assertThat(h2Context).isNotNull();
        assertThat(rb1Context).isNotNull();
        assertThat(rb2Context).isNotNull();
    }

    @Test
    void shouldHandleBpmnMessagesBetweenMulitpleRuntimeBundles() {
        //given
        StartProcessPayload throwProcessPayload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey(INTERMEDIATE_THROW_MESSAGE_PROCESS)
            .withBusinessKey(BUSINESS_KEY)
            .build();

        StartProcessPayload catchProcessPayload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey(INTERMEDIATE_CATCH_MESSAGE_PROCESS)
            .withBusinessKey(BUSINESS_KEY)
            .build();

        //when
        executeCommand(rb1Context, throwProcessPayload);
        executeCommand(rb2Context, throwProcessPayload);
        executeCommand(rb1Context, catchProcessPayload);
        executeCommand(rb2Context, catchProcessPayload);

        //then
        assertThrowCatchBpmnMessages(rb1Context);
        assertThrowCatchBpmnMessages(rb2Context);
    }

    void executeCommand(ConfigurableApplicationContext context, Payload payload) {
        CommandEndpoint<Payload> commandEndpoint = context.getBean(CommandEndpoint.class);
        commandEndpoint.execute(payload);
    }

    void assertThrowCatchBpmnMessages(ConfigurableApplicationContext context) {
        BpmnMessageReceivedEventMessageProducer bpmnMessageReceivedEventMessageProducer = context.getBean(
            BpmnMessageReceivedEventMessageProducer.class
        );
        BpmnMessageSentEventMessageProducer bpmnMessageSentEventMessageProducer = context.getBean(
            BpmnMessageSentEventMessageProducer.class
        );
        BpmnMessageWaitingEventMessageProducer bpmnMessageWaitingEventMessageProducer = context.getBean(
            BpmnMessageWaitingEventMessageProducer.class
        );
        StartMessageCmdExecutor startMessageCmdExecutor = context.getBean(StartMessageCmdExecutor.class);
        ReceiveMessageCmdExecutor receiveMessageCmdExecutor = context.getBean(ReceiveMessageCmdExecutor.class);

        await()
            .untilAsserted(() -> {
                verify(bpmnMessageSentEventMessageProducer, times(1)).onEvent(any());
                verify(bpmnMessageWaitingEventMessageProducer, times(1)).onEvent(any());
                verify(bpmnMessageReceivedEventMessageProducer, times(1)).onEvent(any());

                verify(receiveMessageCmdExecutor, times(1)).execute(any());
                verify(startMessageCmdExecutor, never()).execute(any());
            });
    }
}
