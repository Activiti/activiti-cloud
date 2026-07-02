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
package org.activiti.cloud.starter.tests.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import javax.sql.DataSource;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.services.connectors.recovery.OrphanedIntegrationRecoveryScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

//import org.activiti.services.connectors.recovery.OrphanedIntegrationRecoveryScheduler;

@Testcontainers
class ServiceTaskStartStopBehaviorIT {

    private static final String PROCESS_KEY = "serviceTaskLoop";
    private static final String PROCESS_RESOURCE = "processes/service-task-loop.bpmn20.xml";

    static final AtomicBoolean integrationRequestSent = new AtomicBoolean(false);

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    private static final String[] RABBIT_MQ_PROPERTIES = RabbitMQContainerApplicationInitializer.initialize();
    private static final String[] KEYCLOAK_PROPERTIES;

    static {
        var keycloak = new KeycloakContainerApplicationInitializer();
        keycloak.initialize();
        KEYCLOAK_PROPERTIES = KeycloakContainerApplicationInitializer.getContainerProperties();
    }

    private ConfigurableApplicationContext ctx1;
    private ConfigurableApplicationContext ctx2;

    @AfterEach
    void cleanUp() {
        integrationRequestSent.set(false);
        SecurityContextHolder.clearContext();
        if (ctx1 != null && ctx1.isActive()) {
            ctx1.close();
        }
        if (ctx2 != null && ctx2.isActive()) {
            ctx2.close();
        }
    }

    @ParameterizedTest(name = "functionRouterEnabled={0}")
    @ValueSource(booleans = { false, true })
    void should_taskRemainInStartedState_when_applicationIsKilledDuringServiceTaskExecution(boolean functionRouterEnabled) {
        ctx1 = buildContext(functionRouterEnabled);

        ctx1.getBean(RepositoryService.class).createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

        var processInstanceId = ctx1
            .getBean(RuntimeService.class)
            .createProcessInstanceBuilder()
            .processDefinitionKey(PROCESS_KEY)
            .start()
            .getId();

        await()
            .atMost(Duration.ofSeconds(30))
            .until(integrationRequestSent::get);

        ctx1.close();

        ctx2 = buildContext(functionRouterEnabled);

        setupAdminSecurityContext();

        assertThat(ctx2.getBean(ProcessAdminRuntime.class).processInstance(processInstanceId).getStatus())
            .as("process should be in RUNNING state — not completed by the new instance")
            .isEqualTo(ProcessInstanceStatus.RUNNING);
        assertThat(
            ctx2
                .getBean(RuntimeService.class)
                .createExecutionQuery()
                .processInstanceId(processInstanceId)
                .activityId("LongRunningTask")
                .count()
        )
            .as(
                "service task 'LongRunningTask' should be in STARTED state — execution is waiting for integration result"
            )
            .isEqualTo(1);
        assertThat(ctx2.getBean(ManagementService.class).createJobQuery().count())
            .as(
                "no new async jobs should exist — the process is waiting for the integration result, ctx2 did not re-trigger execution"
            )
            .isZero();

        var jdbcTemplate = new JdbcTemplate(ctx2.getBean(DataSource.class));
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ACT_RU_INTEGRATION WHERE PROCESS_INSTANCE_ID_ = ?",
                Long.class,
                processInstanceId
            )
        )
            .as(
                "an integration context record should exist — the service task was STARTED and is awaiting                 processInstanceId
            )
        )
            .as(
                "an integration context record should exist — the service task was STARTED and is awaiting the integration result"
            )
            .isEqualTo(1L);

//        ctx2.getBean(OrphanedIntegrationRecoveryScheduler.class).recoverOrphanedIntegrations();

            jdbcTemplate.queryForObject(plate.queryForObject(
                        "SELECT COUNT(*) FROM ACT_RU_INTEGRATION WHERE PROCESS_INSTANCE_ID_ = ?",
                        Long.class,
                        processInstanceId
                    )
                )
                    .as(
                        "integration context record should be deleted after recovery — scheduler sent IntegrationError and handler cleaned up"
                    )
                    .isZero();
                setupAdminSecurityContext();
                assertThat(ctx2.getBean(ProcessAdminRuntime.class).processInstance(processInstanceId).getStatus())
                    .as(
                        "process should still be RUNNING after ctx2 has been up — ctx2 must not have completed the service task"
                    )
                    .isEqualTo(ProcessInstanceStatus.RUNNING);
                assertThat(
                    ctx2
                        .getBean(RuntimeService.class)
                        .createExecutionQuery()
                        .processInstanceId(processInstanceId)
                        .activityId("LongRunningTask")
                        .count()
                )
                    .as("service task 'LongRunningTask' should still be STARTED after the recovery")
                    .isEqualTo(1);
            });
    }

    private static void setupAdminSecurityContext() {
        var jwt = Jwt.withTokenValue("test-token").header("alg", "HS256").claim("sub", "admin").build();
        var auth = new JwtAuthenticationToken(
            jwt,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ACTIVITI_ADMIN")),
            "admin"
        );
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private static ConfigurableApplicationContext buildContext(boolean functionRouterEnabled) {
        var rabbitContainer = RabbitMQContainerApplicationInitializer.getContainer();
        TestConfiguration.LOGGER.info(
            "RabbitMQ management UI: http://{}:{} (guest/guest)",
            rabbitContainer.getHost(),
            rabbitContainer.getMappedPort(15672)
        );
        return new SpringApplicationBuilder(RbApplication.class, TestConfiguration.class)
            .web(WebApplicationType.SERVLET)
            .initializers(ctx -> {
                TestPropertyValues.of(RABBIT_MQ_PROPERTIES).applyTo(ctx.getEnvironment());
                TestPropertyValues.of(KEYCLOAK_PROPERTIES).applyTo(ctx.getEnvironment());
                TestPropertyValues.of(
                    "server.port=0",
                    "spring.main.banner-mode=off",
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "activiti.diagram.label.font=0",
                    "spring.activiti.async-executor.seconds-to-wait-on-shutdown=0",
                    "activiti.cloud.runtime-bundle.messaging.required-audit-producer-groups=",
                    "activiti.orphaned-integration-recovery.threshold-minutes=0",
                    "activiti.cloud.messaging.function-router.enabled=" + functionRouterEnabled,
                    "spring.cloud.stream.bindings.longRunningConnectorConsumer.destination=test.longRunningConnector",
                    "spring.cloud.stream.bindings.longRunningConnectorConsumer.group=longRunningConnectorConsumer",
                    "activiti.cloud.messaging.function-router.routes.longRunningConnectorConsumer.enabled=true",
                    "spring.cloud.stream.rabbit.bindings.longRunningConnectorConsumer.consumer.acknowledge-mode=manual"
                ).applyTo(ctx.getEnvironment());
            })
            .run();
    }

    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {}

    interface LongRunningConnectorChannels {
        String CHANNEL_NAME = "longRunningConnectorConsumer";

        @InputBinding(CHANNEL_NAME)
        default SubscribableChannel longRunningConnectorConsumer() {
            return MessageChannels.publishSubscribe(CHANNEL_NAME).getObject();
        }
    }

    @Configuration
    static class TestConfiguration implements LongRunningConnectorChannels {

        private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskStartStopBehaviorIT.class);

        @Bean
        @ConnectorBinding(input = LongRunningConnectorChannels.CHANNEL_NAME, connectorType = "test.longRunningConnector", condition = "")
        ConsumerConnector<IntegrationRequest> longRunningConnector() {
            LOGGER.info("LongRunningConnector bean created");
            return event -> {
                LOGGER.info("LongRunningConnector started for process instance {}", event.getIntegrationContext().getProcessInstanceId());
                integrationRequestSent.set(true);
                for (int i = 1; i <= 30; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.info("LongRunningConnector interrupted at iteration {}", i);
                        return;
                    }
                    LOGGER.info("LongRunningConnector iteration {}/30", i);
                }
                LOGGER.info("LongRunningConnector completed 30s loop without sending a result");
            };
        }
    }
}
