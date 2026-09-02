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
package org.activiti.cloud.services.query.rest.subscriber;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Backstop for sessions whose close was never observed - a half-open socket, a client that
 * vanished without a close frame. A later step wires the interceptor's connection-closed hook
 * to {@link SubscriberRegistry#unregister} for the normal disconnect case; this sweep exists
 * only for when that never fires, and must never be what a working client relies on for timely
 * cleanup.
 */
public class SubscriberSessionExpirySweep {

    private final SubscriberRegistry subscriberRegistry;
    private final Clock clock;
    private final Duration sessionExpiry;

    public SubscriberSessionExpirySweep(SubscriberRegistry subscriberRegistry, Clock clock, Duration sessionExpiry) {
        this.subscriberRegistry = subscriberRegistry;
        this.clock = clock;
        this.sessionExpiry = sessionExpiry;
    }

    @Scheduled(
        fixedRateString = "${query.pushed-counts.session.sweep-interval:PT30S}",
        initialDelayString = "${query.pushed-counts.session.sweep-interval:PT30S}"
    )
    public void sweep() {
        subscriberRegistry.expireSessionsOlderThan(sessionExpiry, clock.instant());
    }
}
