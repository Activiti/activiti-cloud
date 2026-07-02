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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.activiti.services.connectors.recovery.OrphanedIntegrationRecoveryScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
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
import org.springframework.test.util.AopTestUtils;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifies the crash-recovery contract for service tasks: when a connector crashes mid-execution,
 * the process stays RUNNING and {@link OrphanedIntegrationRecoveryScheduler} recovers the orphaned
 * {@code ACT_RU_INTEGRATION} record on the next Runtime Bundle instance.
 *
 * <p>The connector is co-located inside the Runtime Bundle for testing convenience — closing the
 * shared context simulates the connector crash. Two Spring contexts ({@code ctx1}, {@code ctx2})
 * share the same PostgreSQL and RabbitMQ containers, simulating two sequential deployments of the
 * same service. {@code @SpringBootTest} cannot model this restart scenario because it manages a
 * single shared context for the whole test class.
 *
 * <p>Parameterised over {@code functionRouterEnabled} because ACK behaviour differs: with function-router
 * disabled the broker redelivers the unACKed message to ctx2's connector (which also never responds);
 * with function-router enabled the message is ACKed on receipt and lost on crash. Both paths leave an
 * orphaned record that the scheduler must clean up.
 */
@Testcontainers
class ServiceTaskStartStopBehaviorIT {

    private static final String PROCESS_KEY = "serviceTaskLoop";
    private static final String PROCESS_RESOURCE = "processes/service-task-loop.bpmn20.xml";

    // Static so ctx1 and ctx2 — separate Spring contexts in the same JVM — share the same flag.
    static final AtomicBoolean integrationRequestReceived = new AtomicBoolean(false);

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
        integrationRequestReceived.set(false);
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
    void should_taskRemainInStartedState_when_applicationIsKilledDuringServiceTaskExecution(
        boolean functionRouterEnabled
    ) {
        ctx1 = buildContext(functionRouterEnabled);

        ctx1.getBean(RepositoryService.class).createDeployment().addClasspathResource(PROCESS_RESOURCE).deploy();

        var processInstanceId = ctx1
            .getBean(RuntimeService.class)
            .createProcessInstanceBuilder()
            .processDefinitionKey(PROCESS_KEY)
            .start()
            .getId();

        await().atMost(Duration.ofSeconds(30)).until(integrationRequestReceived::get);

        setupAdminSecurityContext();

        assertThat(ctx1.getBean(ProcessAdminRuntime.class).processInstance(processInstanceId).getStatus())
            .as("process should be in RUNNING state while connector is executing")
            .isEqualTo(ProcessInstanceStatus.RUNNING);
        assertThat(
            ctx1
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
        assertThat(ctx1.getBean(ManagementService.class).createJobQuery().count())
            .as("no async jobs should exist — the process is waiting for the integration result, not a timer or retry")
            .isZero();

        var jdbcTemplate = new JdbcTemplate(ctx1.getBean(DataSource.class));
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ACT_RU_INTEGRATION WHERE PROCESS_INSTANCE_ID_ = ?",
                Long.class,
                processInstanceId
            )
        )
            .as("an integration context record should exist — the service task is in flight")
            .isEqualTo(1L);

        // Simulate a hard JVM kill: drop the AMQP connection before Spring's shutdown hooks run so
        // the broker requeues unACKed messages immediately, matching what happens on a real crash.
        ctx1.getBean(CachingConnectionFactory.class).resetConnection();
        ctx1.close();
        // Reset so we can detect whether ctx2's connector receives the requeued message.
        integrationRequestReceived.set(false);

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

        jdbcTemplate = new JdbcTemplate(ctx2.getBean(DataSource.class));
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ACT_RU_INTEGRATION WHERE PROCESS_INSTANCE_ID_ = ?",
                Long.class,
                processInstanceId
            )
        )
            .as(
                "an integration context record should exist — the service task was STARTED and is awaiting the integration result"
            )
            .isEqualTo(1L);

        if (functionRouterEnabled) {
            // Function router ACKs the message on receipt before routing it to the connector.
            // On crash the message is already gone — ctx2's connector should never receive it.
            await()
                .during(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(6))
                .until(() -> !integrationRequestReceived.get());
        } else {
            // MANUAL ACK + resetConnection causes the broker to redeliver the message to ctx2.
            await().atMost(Duration.ofSeconds(30)).until(integrationRequestReceived::get);
        }

        ctx2.getBean(OrphanedIntegrationRecoveryScheduler.class).recoverOrphanedIntegrations();

        var errorCaptor = ArgumentCaptor.forClass(IntegrationError.class);
        verify(
            AopTestUtils.<ServiceTaskIntegrationErrorEventHandler>getTargetObject(
                ctx2.getBean(ServiceTaskIntegrationErrorEventHandler.class)
            )
        ).receive(errorCaptor.capture());
        assertThat(errorCaptor.getValue()).satisfies(error -> {
            assertThat(error.getErrorClassName()).isEqualTo(RuntimeException.class.getName());
            assertThat(error.getErrorMessage()).isEqualTo(
                OrphanedIntegrationRecoveryScheduler.ORPHANED_INTEGRATION_ERROR_MESSAGE
            );
        });
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ACT_RU_INTEGRATION WHERE PROCESS_INSTANCE_ID_ = ?",
                Long.class,
                processInstanceId
            )
        )
            .as(
                "integration context record should be deleted after recovery — scheduler sent IntegrationError and handler cleaned up"
            )
            .isZero();

        await()
            .pollDelay(Duration.ofSeconds(10))
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
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
                    // MANUAL ACK prevents a reconnection race: after resetConnection() the listener container
                    // tries to reconnect before ctx1.close() stops it; if reconnection succeeds and the
                    // requeued message is re-delivered, AUTO ACK would fire on interrupt-return and lose it.
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

        static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskStartStopBehaviorIT.class);

        @Bean
        ServiceTaskIntegrationErrorEventHandler serviceTaskIntegrationErrorEventHandler(
            RuntimeService runtimeService,
            IntegrationContextService integrationContextService,
            ManagementService managementService,
            RuntimeBundleProperties runtimeBundleProperties,
            ProcessEngineEventsAggregator processEngineEventsAggregator
        ) {
            return spy(
                new ServiceTaskIntegrationErrorEventHandler(
                    runtimeService,
                    integrationContextService,
                    managementService,
                    runtimeBundleProperties,
                    processEngineEventsAggregator
                )
            );
        }

        @SuppressWarnings("java:S2925")
        @Bean
        @ConnectorBinding(
            input = LongRunningConnectorChannels.CHANNEL_NAME,
            connectorType = "test.longRunningConnector",
            condition = "" // empty bypasses the default appVersion header check
        )
        ConsumerConnector<IntegrationRequest> longRunningConnector() {
            return event -> {
                LOGGER.info(
                    "LongRunningConnector started for process instance {}",
                    event.getIntegrationContext().getProcessInstanceId()
                );
                integrationRequestReceived.set(true);
                for (int i = 1; i <= 30; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException _) {
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
