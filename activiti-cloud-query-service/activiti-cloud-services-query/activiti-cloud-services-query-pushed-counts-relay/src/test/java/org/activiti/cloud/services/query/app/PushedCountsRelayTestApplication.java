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

import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.activiti.cloud.services.query.subscription.SubscriberRegistrySnapshot;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;

/**
 * Minimal host for the relay auto-configuration under the in-memory test binder. Supplies the three
 * collaborators query-rest would normally provide - the pushed-counts sink, the feature toggle and a
 * {@link SubscriberRegistrySnapshot} standing in for the local subscriber registry - so the binding
 * tests can exercise the messaging path without pulling in the full query-rest starter.
 */
@SpringBootApplication
public class PushedCountsRelayTestApplication {

    @Bean
    Sinks.Many<CountChangedMessage> pushedCountsSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    FeatureToggle featureToggle() {
        return name -> false;
    }

    @Bean
    SubscriberRegistrySnapshot subscriberRegistrySnapshot() {
        return () -> List.of(new SubscriberRegistryMessage.Entry("alice", List.of("eng")));
    }
}
