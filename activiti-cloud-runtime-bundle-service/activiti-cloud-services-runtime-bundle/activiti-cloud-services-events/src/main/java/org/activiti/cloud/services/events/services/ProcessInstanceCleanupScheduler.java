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
package org.activiti.cloud.services.events.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.activiti.cloud.services.core.conf.ProcessCleanupProperties;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class ProcessInstanceCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceCleanupScheduler.class);

    private final HistoryService historyService;
    private final CloudProcessDeletedService cloudProcessDeletedService;
    private final ProcessCleanupProperties cleanupProperties;

    public ProcessInstanceCleanupScheduler(
        HistoryService historyService,
        CloudProcessDeletedService cloudProcessDeletedService,
        ProcessCleanupProperties cleanupProperties
    ) {
        this.historyService = historyService;
        this.cloudProcessDeletedService = cloudProcessDeletedService;
        this.cleanupProperties = cleanupProperties;
    }

    @Scheduled(fixedDelayString = "#{@processCleanupProperties.cleanupInterval.toMillis()}")
    public void cleanupOldProcessInstances() {
        LOGGER.debug("Starting scheduled cleanup of old process instances");

        try {
            Date thresholdDate = Date.from(Instant.now().minus(cleanupProperties.getGracePeriod()));
            List<HistoricProcessInstance> completedInstances = historyService
                .createHistoricProcessInstanceQuery()
                .finished()
                .finishedBefore(thresholdDate)
                .listPage(0, cleanupProperties.getBatchSize());

            if (completedInstances.isEmpty()) {
                LOGGER.debug("No process instances found for cleanup");
                return;
            }

            LOGGER.info(
                "Found {} completed process instances older than {} minutes to delete",
                completedInstances.size(),
                cleanupProperties.getGracePeriod().toMinutes()
            );

            for (HistoricProcessInstance completedInstance : completedInstances) {
                try {
                    cloudProcessDeletedService.delete(completedInstance.getId());
                    LOGGER.debug("Deleted process instance: {}", completedInstance.getId());
                } catch (Exception ex) {
                    LOGGER.error(
                        "Failed to delete process instance {}: {}",
                        completedInstance.getId(),
                        ex.getMessage(),
                        ex
                    );
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error during scheduled process instance cleanup", ex);
        }
    }
}
