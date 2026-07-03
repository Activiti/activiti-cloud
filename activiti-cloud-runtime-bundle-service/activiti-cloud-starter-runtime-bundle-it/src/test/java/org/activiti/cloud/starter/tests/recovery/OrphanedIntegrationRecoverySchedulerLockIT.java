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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.services.connectors.recovery.OrphanedIntegrationRecoveryScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifies that ShedLock prevents concurrent execution of {@link OrphanedIntegrationRecoveryScheduler}
 * across two Runtime Bundle instances sharing the same database.
 *
 * <p>Two Spring contexts ({@code ctx1}, {@code ctx2}) share the same PostgreSQL container,
 * which means they share the {@code shedlock_runtimebundle} lock table. When ctx1 holds the
 * lock, ctx2's scheduler call must be silently skipped by the ShedLock proxy.
 */
@Testcontainers
class OrphanedIntegrationRecoverySchedulerLockIT {

    private static final String LOCK_NAME = "orphanedIntegrationRecovery";

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
        if (ctx1 != null && ctx1.isActive()) {
            ctx1.close();
        }
        if (ctx2 != null && ctx2.isActive()) {
            ctx2.close();
        }
    }

    @Test
    void should_runRecoveryOnlyOnce_when_twoInstancesRunConcurrently() {
        ctx1 = buildContext();
        ctx2 = buildContext();

        var lockConfig = new LockConfiguration(Instant.now(), LOCK_NAME, Duration.ofMinutes(5), Duration.ZERO);

        // ctx1 acquires the lock, simulating ctx1's scheduler starting execution
        var ctx1Lock = ctx1.getBean(LockProvider.class).lock(lockConfig);
        assertThat(ctx1Lock).as("ctx1 should acquire the scheduler lock").isPresent();

        // ctx2 uses the same shedlock_runtimebundle table, so it cannot acquire the same lock
        assertThat(ctx2.getBean(LockProvider.class).lock(lockConfig))
            .as("ctx2 should not acquire the lock while ctx1 holds it")
            .isEmpty();

        // ctx2 calls recoverOrphanedIntegrations() — the ShedLock proxy detects the held lock and
        // skips the method body without throwing; lock_until in the table must remain unchanged
        var jdbcTemplate = new JdbcTemplate(ctx2.getBean(DataSource.class));
        var lockUntilBefore = lockUntil(jdbcTemplate);
        ctx2.getBean(OrphanedIntegrationRecoveryScheduler.class).recoverOrphanedIntegrations();
        assertThat(lockUntil(jdbcTemplate))
            .as("lock_until must not change — ctx2 scheduler was skipped by ShedLock")
            .isEqualTo(lockUntilBefore);

        // ctx1 releases the lock
        ctx1Lock.get().unlock();

        // ctx2 can now acquire the lock (scheduler will run on the next invocation)
        assertThat(ctx2.getBean(LockProvider.class).lock(lockConfig))
            .as("ctx2 should acquire the lock after ctx1 releases it")
            .isPresent();
    }

    private static Timestamp lockUntil(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
            "SELECT lock_until FROM shedlock_runtimebundle WHERE name = ?",
            Timestamp.class,
            LOCK_NAME
        );
    }

    private static ConfigurableApplicationContext buildContext() {
        return new SpringApplicationBuilder(RbApplication.class)
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
                    "activiti.orphaned-integration-recovery.cron=0 0 0 1 1 *"
                ).applyTo(ctx.getEnvironment());
            })
            .run();
    }

    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {}
}
