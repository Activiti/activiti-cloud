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
package org.activiti.cloud.services.query.app;

import java.util.StringJoiner;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * FOR MANUAL TESTING ONLY — TO BE REMOVED. Not part of the feature: it exists solely to make the
 * in-memory subscriber registry observable on a running environment (the registry is not exposed
 * anywhere else). Delete this class and its bean in {@code PushedCountsAutoConfiguration} before
 * this ships.
 */
public class SubscriberRegistryStateLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistryStateLogger.class);

    private final ConsumerSubscriberRegistry registry;
    private final FeatureToggle featureToggle;
    private String lastLogged = "";

    public SubscriberRegistryStateLogger(ConsumerSubscriberRegistry registry, FeatureToggle featureToggle) {
        this.registry = registry;
        this.featureToggle = featureToggle;
    }

    @Scheduled(fixedDelayString = "${activiti.cloud.query.pushed-counts.debug.log-interval:PT5S}")
    public void logWhenChanged() {
        if (!featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            return;
        }
        String snapshot = render();
        if (!snapshot.equals(lastLogged)) {
            lastLogged = snapshot;
            LOGGER.info("[pushed-counts] subscriber registry {}", snapshot);
        }
    }

    private String render() {
        var watchedUserIds = registry.watchedUserIds();
        StringJoiner users = new StringJoiner(", ", "[", "]");
        watchedUserIds
            .stream()
            .sorted()
            .forEach(userId ->
                users.add(userId + " groups=" + registry.groupsOf(userId) + " sources=" + registry.sourcesOf(userId))
            );
        return "size=" + watchedUserIds.size() + " " + users;
    }
}
