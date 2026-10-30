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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

class OrphanedIntegrationRecoveryPropertiesTest {

    private final OrphanedIntegrationRecoveryProperties properties = new OrphanedIntegrationRecoveryProperties();

    @Test
    void should_beActive_byDefault() {
        assertThat(properties.isActive()).isTrue();
    }

    @Test
    void should_useProjectReleaseVolume_byDefault() {
        assertThat(properties.getProjectReleaseDir()).isEqualTo("/home/alfresco/.activiti/project-release-volume/");
    }

    @Test
    void should_beInactive_when_cronIsDisabled() {
        properties.setCron(ScheduledTaskRegistrar.CRON_DISABLED);

        assertThat(properties.isActive()).isFalse();
    }

    @Test
    void should_bindConnectorThresholdsAndProjectReleaseDir() {
        var source = new MapConfigurationPropertySource(
            Map.of(
                "activiti.orphaned-integration-recovery.connector-thresholds[agenticTaskConnector]",
                "-1",
                "activiti.orphaned-integration-recovery.connector-thresholds[restConnector]",
                "600",
                "activiti.orphaned-integration-recovery.project-release-dir",
                "/opt/project-release/"
            )
        );

        var bound = new Binder(source)
            .bind("activiti.orphaned-integration-recovery", OrphanedIntegrationRecoveryProperties.class)
            .get();

        assertThat(bound.getConnectorThresholds()).containsExactlyInAnyOrderEntriesOf(
            Map.of("agenticTaskConnector", -1, "restConnector", 600)
        );
        assertThat(bound.getProjectReleaseDir()).isEqualTo("/opt/project-release/");
    }
}
