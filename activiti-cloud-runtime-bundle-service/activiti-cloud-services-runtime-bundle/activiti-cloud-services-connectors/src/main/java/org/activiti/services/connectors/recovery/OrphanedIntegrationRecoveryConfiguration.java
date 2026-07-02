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
package org.activiti.services.connectors.recovery;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(
    value = "activiti.orphaned-integration-recovery.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@EnableConfigurationProperties(OrphanedIntegrationRecoveryProperties.class)
public class OrphanedIntegrationRecoveryConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock_runtimebundle")
                .usingDbTime()
                .build()
        );
    }

    @Bean
    OrphanedIntegrationRecoveryScheduler orphanedIntegrationRecoveryScheduler(
        IntegrationContextService integrationContextService,
        IntegrationRequestBuilder integrationRequestBuilder,
        ServiceTaskIntegrationErrorEventHandler errorEventHandler,
        OrphanedIntegrationRecoveryProperties properties
    ) {
        return new OrphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            properties
        );
    }
}
