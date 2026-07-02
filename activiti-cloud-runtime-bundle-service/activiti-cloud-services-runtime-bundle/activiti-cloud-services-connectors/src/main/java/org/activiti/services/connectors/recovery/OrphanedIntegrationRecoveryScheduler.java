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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

public class OrphanedIntegrationRecoveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedIntegrationRecoveryScheduler.class);

    public static final String ORPHANED_INTEGRATION_ERROR_MESSAGE =
        "Service task did not complete: the application instance was shut down while this integration was in progress.";

    private final JdbcTemplate jdbcTemplate;
    private final IntegrationContextService integrationContextService;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final ServiceTaskIntegrationErrorEventHandler errorEventHandler;
    private final OrphanedIntegrationRecoveryProperties properties;

    OrphanedIntegrationRecoveryScheduler(
        JdbcTemplate jdbcTemplate,
        IntegrationContextService integrationContextService,
        IntegrationRequestBuilder integrationRequestBuilder,
        ServiceTaskIntegrationErrorEventHandler errorEventHandler,
        OrphanedIntegrationRecoveryProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.integrationContextService = integrationContextService;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.errorEventHandler = errorEventHandler;
        this.properties = properties;
    }

    @Scheduled(cron = "${activiti.orphaned-integration-recovery.cron:0 */5 * * * *}")
    @SchedulerLock(name = "orphanedIntegrationRecovery")
    public void recoverOrphanedIntegrations() {
        var threshold = Date.from(Instant.now().minus(properties.getThresholdMinutes(), ChronoUnit.MINUTES));
        var ids = findOrphanedIntegrationContextIds(threshold);

        if (ids.isEmpty()) {
            return;
        }

        LOGGER.warn(
            "Found {} orphaned integration context(s) older than {} minutes. Sending integration errors.",
            ids.size(),
            properties.getThresholdMinutes()
        );

        for (var id : ids) {
            recoverOrphanedIntegration(id);
        }
    }

    private List<String> findOrphanedIntegrationContextIds(Date threshold) {
        return jdbcTemplate.queryForList(
            "SELECT ID_ FROM ACT_RU_INTEGRATION WHERE CREATED_DATE_ < ?",
            String.class,
            threshold
        );
    }

    private void recoverOrphanedIntegration(String id) {
        var entity = integrationContextService.findById(id);
        if (entity == null) {
            return;
        }
        try {
            var integrationContext = new IntegrationContextImpl();
            integrationContext.setId(entity.getId());
            integrationContext.setExecutionId(entity.getExecutionId());
            integrationContext.setProcessInstanceId(entity.getProcessInstanceId());
            integrationContext.setProcessDefinitionId(entity.getProcessDefinitionId());

            var integrationRequest = integrationRequestBuilder.build(integrationContext);
            var integrationError = new IntegrationErrorImpl(
                integrationRequest,
                new RuntimeException(ORPHANED_INTEGRATION_ERROR_MESSAGE)
            );

            errorEventHandler.receive(integrationError);

            LOGGER.info(
                "Sent integration error for orphaned context {} (process instance {}).",
                id,
                entity.getProcessInstanceId()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to recover orphaned integration context {}.", id, e);
        }
    }
}
