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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
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

    private final IntegrationContextService integrationContextService;
    private final IntegrationRequestBuilder integrationRequestBuilder;
    private final ServiceTaskIntegrationErrorEventHandler errorEventHandler;
    private final int thresholdSeconds;
    /** Thresholds by connector template, matched case-sensitively. */
    private final Map<String, Integer> connectorThresholds;
    private final FeatureToggle featureToggle;
    private final ConnectorTemplateResolver connectorTemplateResolver;

    OrphanedIntegrationRecoveryScheduler(
        IntegrationContextService integrationContextService,
        IntegrationRequestBuilder integrationRequestBuilder,
        ServiceTaskIntegrationErrorEventHandler errorEventHandler,
        int thresholdSeconds,
        Map<String, Integer> connectorThresholds,
        FeatureToggle featureToggle,
        ConnectorTemplateResolver connectorTemplateResolver
    ) {
        this.integrationContextService = integrationContextService;
        this.integrationRequestBuilder = integrationRequestBuilder;
        this.errorEventHandler = errorEventHandler;
        this.thresholdSeconds = thresholdSeconds;
        this.connectorThresholds = connectorThresholds;
        this.featureToggle = featureToggle;
        this.connectorTemplateResolver = connectorTemplateResolver;
    }

    @Scheduled(
        cron = "${activiti.orphaned-integration-recovery.cron:" +
            OrphanedIntegrationRecoveryProperties.DEFAULT_CRON +
            "}"
    )
    @SchedulerLock(name = "orphanedIntegrationRecovery")
    public void recoverOrphanedIntegrations() {
        LOGGER.debug("Orphaned integration recovery job started.");
        if (!featureToggle.isEnabled(RuntimeBundleFeatureToggles.ORPHANED_INTEGRATION_RECOVERY)) {
            LOGGER.debug("Orphaned integration recovery is disabled via feature toggle. Skipping.");
            return;
        }
        var now = Instant.now();
        var threshold = Date.from(now.minus(minThresholdSeconds(), ChronoUnit.SECONDS));
        var candidates = integrationContextService.createIntegrationContextQuery().createdBefore(threshold).list();

        if (candidates.isEmpty()) {
            LOGGER.debug("Orphaned integration recovery job completed. No orphaned contexts found.");
            return;
        }

        LOGGER.debug("Found {} candidate orphaned integration context(s).", candidates.size());

        candidates.forEach(entity -> recoverOrphanedIntegration(entity, now));
        LOGGER.debug("Orphaned integration recovery job completed. Processed {} context(s).", candidates.size());
    }

    /**
     * The query must include contexts eligible for the lowest configured threshold,
     * each context is then checked against the threshold of its own connector template.
     */
    private int minThresholdSeconds() {
        return connectorThresholds
            .values()
            .stream()
            .filter(value -> value != null && value >= 0)
            .reduce(thresholdSeconds, Math::min);
    }

    private void recoverOrphanedIntegration(IntegrationContextEntity entity, Instant now) {
        try {
            var entityThresholdSeconds = resolveThresholdSeconds(entity);
            if (entityThresholdSeconds < 0) {
                return;
            }

            var elapsed = Duration.between(entity.getCreatedDate().toInstant(), now);
            if (elapsed.getSeconds() < entityThresholdSeconds) {
                LOGGER.debug(
                    "Skipping integration context {}: started {} seconds ago, threshold is {} seconds.",
                    entity.getId(),
                    elapsed.getSeconds(),
                    entityThresholdSeconds
                );
                return;
            }

            LOGGER.warn(
                "Integration context {} (flow node {}) is orphaned: started {} seconds ago, threshold is {} seconds. Sending integration error.",
                entity.getId(),
                entity.getFlowNodeId(),
                elapsed.getSeconds(),
                entityThresholdSeconds
            );

            var integrationContext = new IntegrationContextImpl();
            integrationContext.setId(entity.getId());
            integrationContext.setExecutionId(entity.getExecutionId());
            integrationContext.setProcessInstanceId(entity.getProcessInstanceId());
            integrationContext.setProcessDefinitionId(entity.getProcessDefinitionId());
            integrationContext.setClientId(entity.getFlowNodeId());

            var integrationRequest = integrationRequestBuilder.build(integrationContext);
            var integrationError = new IntegrationErrorImpl(
                integrationRequest,
                new CloudBpmnError(ORPHANED_INTEGRATION_ERROR_CODE, buildErrorMessage(entityThresholdSeconds))
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

    private int resolveThresholdSeconds(IntegrationContextEntity entity) {
        if (connectorThresholds.isEmpty()) {
            return thresholdSeconds;
        }
        String template;
        try {
            template = connectorTemplateResolver
                .resolveTemplate(entity.getProcessDefinitionId(), entity.getFlowNodeId())
                .orElse(null);
        } catch (Exception e) {
            LOGGER.warn(
                "Unable to resolve the connector template of integration context {}, using the default threshold.",
                entity.getId(),
                e
            );
            return thresholdSeconds;
        }
        var connectorThreshold = template == null ? null : connectorThresholds.get(template);
        if (connectorThreshold == null) {
            return thresholdSeconds;
        }
        if (connectorThreshold < 0) {
            LOGGER.debug(
                "Integration context {} (flow node {}) will not be recovered: connector template {} is excluded from the orphaned integration recovery.",
                entity.getId(),
                entity.getFlowNodeId(),
                template
            );
        }
        return connectorThreshold;
    }

    private String buildErrorMessage(int thresholdSeconds) {
        return "Service task did not complete: the integration was not resolved within the expected time (threshold: %s). Possible causes include application shutdown, connector crash, or task interruption.".formatted(
            formatThreshold(thresholdSeconds)
        );
    }

    private static String formatThreshold(int thresholdSeconds) {
        var minutes = thresholdSeconds / 60;
        var seconds = thresholdSeconds % 60;
        if (minutes == 0) return seconds + " sec";
        if (seconds == 0) return minutes + " min";
        return minutes + " min " + seconds + " sec";
    }
}
