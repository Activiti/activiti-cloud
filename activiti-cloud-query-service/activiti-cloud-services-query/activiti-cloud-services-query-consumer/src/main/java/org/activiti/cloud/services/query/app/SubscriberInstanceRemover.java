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

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes query-rest instances that have stopped sending heartbeats, dropping any user left with no
 * live instance. Meant to be invoked periodically; the schedule is kept external so the timeout and
 * the clock stay injectable and the removal decision remains deterministically testable.
 */
public class SubscriberInstanceRemover {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberInstanceRemover.class);

    private final ConsumerSubscriberRegistry registry;
    private final Duration instanceTimeout;
    private final Clock clock;

    public SubscriberInstanceRemover(ConsumerSubscriberRegistry registry, Duration instanceTimeout, Clock clock) {
        this.registry = registry;
        this.instanceTimeout = instanceTimeout;
        this.clock = clock;
    }

    public Set<String> removeExpiredInstances() {
        Set<String> droppedUsers = registry.expireInstances(clock.instant(), instanceTimeout);
        if (!droppedUsers.isEmpty()) {
            LOGGER.debug(
                "Dropped {} subscriber(s) after instance-liveness expiry: {}",
                droppedUsers.size(),
                droppedUsers
            );
        }
        return droppedUsers;
    }
}
