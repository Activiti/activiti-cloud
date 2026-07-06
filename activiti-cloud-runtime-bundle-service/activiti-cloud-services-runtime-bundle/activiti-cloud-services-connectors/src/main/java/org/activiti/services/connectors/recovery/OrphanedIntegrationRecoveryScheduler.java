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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class OrphanedIntegrationRecoveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedIntegrationRecoveryScheduler.class);

    public static final String ORPHANED_INTEGRATION_ERROR_CODE = "ORPHANED_INTEGRATION";

    public static final String ORPHANED_INTEGRATION_ERROR_MESSAGE =
        "Service task did not complete: the integration was not resolved within the expected time." +
        " Possible causes include application shutdown, connector crash, or task interruption.";

    private final IntegrationContextService integrationContextService;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final ServiceTaskIntegrationErrorEventHandler errorEventHandler;
    private final OrphanedIntegrationRecoveryProperties properties;
    private final FeatureToggle featureToggle;

    OrphanedIntegrationRecoveryScheduler(
        IntegrationContextService integrationContextService,
        IntegrationRequestBuilder integrationRequestBuilder,
        ServiceTaskIntegrationErrorEventHandler errorEventHandler,
        OrphanedIntegrationRecoveryProperties properties,
        FeatureToggle featureToggle
    ) {
        this.integrationContextService = integrationContextService;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.errorEventHandler = errorEventHandler;
        this.properties = properties;
        this.featureToggle = featureToggle;
    }

    @Scheduled(cron = "${activiti.orphaned-integration-recovery.cron:0 */5 * * * *}")
    @SchedulerLock(name = "orphanedIntegrationRecovery")
    public void recoverOrphanedIntegrations() {
        LOGGER.debug("Orphaned integration recovery job started.");
        if (!featureToggle.isEnabled(RuntimeBundleFeatureToggles.ORPHANED_INTEGRATION_RECOVERY)) {
            LOGGER.debug("Orphaned integration recovery is disabled via feature toggle. Skipping.");
            return;
        }
        var threshold = Date.from(Instant.now().minus(properties.getThresholdSeconds(), ChronoUnit.SECONDS));
        var orphaned = integrationContextService.createIntegrationContextQuery().createdBefore(threshold).list();

        if (orphaned.isEmpty()) {
            LOGGER.debug("Orphaned integration recovery job completed. No orphaned contexts found.");
            return;
        }

        LOGGER.warn(
            "Found {} orphaned integration context(s) older than {} seconds. Sending integration errors.",
            orphaned.size(),
            properties.getThresholdSeconds()
        );

        orphaned.forEach(this::recoverOrphanedIntegration);
        LOGGER.debug("Orphaned integration recovery job completed. Processed {} context(s).", orphaned.size());
    }

    private void recoverOrphanedIntegration(IntegrationContextEntity entity) {
        try {
            var integrationContext = new IntegrationContextImpl();
            integrationContext.setId(entity.getId());
            integrationContext.setExecutionId(entity.getExecutionId());
            integrationContext.setProcessInstanceId(entity.getProcessInstanceId());
            integrationContext.setProcessDefinitionId(entity.getProcessDefinitionId());
            integrationContext.setClientId(entity.getFlowNodeId());

            var integrationRequest = integrationRequestBuilder.build(integrationContext);
            var integrationError = new IntegrationErrorImpl(
                integrationRequest,
                new CloudBpmnError(ORPHANED_INTEGRATION_ERROR_CODE, ORPHANED_INTEGRATION_ERROR_MESSAGE)
            );

            errorEventHandler.receive(integrationError);

            LOGGER.info(
                "Sent integration error for orphaned context {} (process instance {}).",
                entity.getId(),
                entity.getProcessInstanceId()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to recover orphaned integration context {}.", entity.getId(), e);
        }
    }
}
