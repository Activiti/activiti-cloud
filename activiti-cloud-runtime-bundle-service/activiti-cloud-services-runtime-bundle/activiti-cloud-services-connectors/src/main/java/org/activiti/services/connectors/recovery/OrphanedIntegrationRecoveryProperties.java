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

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@ConfigurationProperties("activiti.orphaned-integration-recovery")
public class OrphanedIntegrationRecoveryProperties {

    public static final String DEFAULT_CRON = "0 */5 * * * *";

    public static final String DEFAULT_PROJECT_RELEASE_DIR = "/home/alfresco/.activiti/project-release-volume/";

    private int thresholdSeconds = 10800; // 3 hours

    private String cron = DEFAULT_CRON;

    /**
     * Thresholds in seconds by connector template, overriding {@link #thresholdSeconds}.
     * A negative value excludes the template from the recovery.
     */
    private Map<String, Integer> connectorThresholds = new HashMap<>();

    /**
     * Directory containing a sub-directory for each project release version,
     * the connector definitions are read from {@code <projectReleaseDir>/<version>/connectors/}.
     */
    private String projectReleaseDir = DEFAULT_PROJECT_RELEASE_DIR;

    public int getThresholdSeconds() {
        return thresholdSeconds;
    }

    public void setThresholdSeconds(int thresholdSeconds) {
        this.thresholdSeconds = thresholdSeconds;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Map<String, Integer> getConnectorThresholds() {
        return connectorThresholds;
    }

    public void setConnectorThresholds(Map<String, Integer> connectorThresholds) {
        this.connectorThresholds = connectorThresholds;
    }

    public String getProjectReleaseDir() {
        return projectReleaseDir;
    }

    public void setProjectReleaseDir(String projectReleaseDir) {
        this.projectReleaseDir = projectReleaseDir;
    }

    public boolean isActive() {
        return !ScheduledTaskRegistrar.CRON_DISABLED.equals(cron);
    }
}
