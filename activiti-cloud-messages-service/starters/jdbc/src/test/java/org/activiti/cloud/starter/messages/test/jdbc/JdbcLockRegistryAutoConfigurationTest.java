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
package org.activiti.cloud.starter.messages.test.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.core.config.MessageAggregatorProperties;
import org.activiti.cloud.starter.messages.jdbc.JdbcMessageStoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Asserts that {@link JdbcMessageStoreAutoConfiguration} registers a {@link JdbcLockRegistry} (same beans as production
 * when this starter is used). This uses the real auto-configuration class — not a test-only {@code LockRegistry} bean.
 */
class JdbcLockRegistryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                JdbcTemplateAutoConfiguration.class,
                IntegrationAutoConfiguration.class,
                JdbcMessageStoreAutoConfiguration.class
            )
        )
        .withUserConfiguration(MessageAggregatorPropertiesConfiguration.class)
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:lockRegistryAutoCfg;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.integration.jdbc.initialize-schema=always",
            "spring.sql.init.platform=h2"
        );

    @Configuration
    @EnableConfigurationProperties(MessageAggregatorProperties.class)
    static class MessageAggregatorPropertiesConfiguration {}

    @Test
    void jdbcMessageStoreAutoConfigurationShouldRegisterJdbcLockRegistry() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LockRegistry.class);
            assertThat(context.getBean(LockRegistry.class)).isInstanceOf(JdbcLockRegistry.class);
        });
    }
}
