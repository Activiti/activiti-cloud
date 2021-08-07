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
package org.activiti.cloud.starter.tests.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import org.activiti.cloud.services.job.executor.JobMessageHandler;
import org.activiti.cloud.services.job.executor.JobMessageHandlerFactory;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessageHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

@Testcontainers
class MultipleRbMessagesIT {

    private static final Logger logger = LoggerFactory.getLogger(MultipleRbMessagesIT.class);

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
        public JobMessageHandlerFactory jobMessageHandlerFactory() {
            return new JobMessageHandlerFactory() {

                @Override
                public MessageHandler create(ProcessEngineConfigurationImpl configuration) {
                    return spy(new JobMessageHandler(configuration));
                }
            };
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
    void shouldHandleBpmnMessagesBetweenMulitpleRuntimeBundles() throws InterruptedException {
        logger.info("shouldHandleBpmnMessagesBetweenMulitpleRuntimeBundles");
        //given
        //when
        //then
    }
}
