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

import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.app.SubscriberInstanceRemovalScheduler;
import org.activiti.cloud.services.query.app.SubscriberInstanceRemover;
import org.activiti.cloud.services.query.app.SubscriberRegistryConsumer;
import org.activiti.cloud.services.query.app.SubscriberRegistryMessageHandler;
import org.activiti.cloud.services.query.app.SubscriberRegistryResyncRequester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.MessageChannel;

class PushedCountsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context ->
            context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
        .withBean(FeatureToggle.class, () -> name -> false)
        .withBean("subscriberRegistryProducer", MessageChannel.class, () -> new NullChannel())
        .withConfiguration(AutoConfigurations.of(PushedCountsAutoConfiguration.class));

    @Test
    void registersRegistryHandlerAndFeatureGatedConsumerFunction() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConsumerSubscriberRegistry.class);
            assertThat(context).hasSingleBean(SubscriberRegistryMessageHandler.class);
            assertThat(context).hasSingleBean(SubscriberInstanceRemover.class);
            assertThat(context).hasSingleBean(SubscriberInstanceRemovalScheduler.class);
            assertThat(context).hasSingleBean(SubscriberRegistryResyncRequester.class);
            assertThat(context).hasBean("subscriberRegistryConsumerFunction");
            assertThat(context.getBean("subscriberRegistryConsumerFunction"))
                .isInstanceOf(SubscriberRegistryConsumer.class);
        });
    }

    @Test
    void backsOffRegistry_whenOneIsAlreadyDefined() {
        ConsumerSubscriberRegistry existing = new ConsumerSubscriberRegistry();

        contextRunner
            .withBean("customRegistry", ConsumerSubscriberRegistry.class, () -> existing)
            .run(context -> {
                assertThat(context).hasSingleBean(ConsumerSubscriberRegistry.class);
                assertThat(context.getBean(ConsumerSubscriberRegistry.class)).isSameAs(existing);
            });
    }
}
