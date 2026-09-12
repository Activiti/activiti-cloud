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
package org.activiti.cloud.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistry;
import org.activiti.cloud.services.query.rest.subscriber.SubscriberRegistryBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;

class QueryRestPushedCountsBridgeAutoConfigurationTest {

    private static final String RESYNC_RESPONDER_BEAN = "subscriberRegistryResyncResponder";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(QueryRestPushedCountsBridgeAutoConfiguration.class))
        .withBean(StreamBridge.class, () -> mock(StreamBridge.class))
        .withBean(Clock.class, Clock::systemUTC)
        .withBean(SubscriberRegistry.class, () ->
            new SubscriberRegistry(mock(ApplicationEventPublisher.class), 50_000)
        );

    @Test
    void should_wireTheBroadcasterAndResyncResponder_when_pushedCountsIsEnabled() {
        contextRunner
            .withPropertyValues("activiti.cloud.query.pushed-counts.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(SubscriberRegistryBroadcaster.class);
                assertThat(context).hasBean(RESYNC_RESPONDER_BEAN);
            });
    }

    @Test
    void should_wireNothing_when_pushedCountsIsExplicitlyDisabled() {
        contextRunner
            .withPropertyValues("activiti.cloud.query.pushed-counts.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(SubscriberRegistryBroadcaster.class);
                assertThat(context).doesNotHaveBean(RESYNC_RESPONDER_BEAN);
            });
    }

    @Test
    void should_wireNothing_when_pushedCountsPropertyIsAbsent() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SubscriberRegistryBroadcaster.class);
            assertThat(context).doesNotHaveBean(RESYNC_RESPONDER_BEAN);
        });
    }
}
