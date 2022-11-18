/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

package org.activiti.cloud.common.messaging.config;

import static org.activiti.cloud.common.messaging.config.ActivitiMessagingEnvironmentPostProcessor.MANAGEMENT_HEALTH_RABBIT_ENABLED_KEY;
import static org.activiti.cloud.common.messaging.config.ActivitiMessagingEnvironmentPostProcessor.SPRING_CLOUD_STREAM_DEFAULT_BINDER_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties.MessagingBroker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

public class ActivitiMessagingEnvironmentPostProcessorTest {

    private final ActivitiMessagingEnvironmentPostProcessor processor = new ActivitiMessagingEnvironmentPostProcessor();

    @Test
    public void should_setDefaultBinderToRabbit_when_brokerIsRabbitmq() {
        //given
        final MutablePropertySources propertySources = mock(MutablePropertySources.class);
        final ConfigurableEnvironment environment = buildEnvironment(MessagingBroker.rabbitmq, propertySources);
        final ArgumentCaptor<MapPropertySource> captor = ArgumentCaptor.forClass(MapPropertySource.class);

        //when
        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        //then
        verify(propertySources).addAfter(eq(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME), captor.capture());
        assertThat(captor.getValue().getProperty(SPRING_CLOUD_STREAM_DEFAULT_BINDER_KEY)).isEqualTo("rabbit");
    }

    @Test
    public void should_setDefaultBinderToKafkaAndDisableRabbitHelfCheck_when_brokerIsKafka() {
        //given
        final MutablePropertySources propertySources = mock(MutablePropertySources.class);
        final ConfigurableEnvironment environment = buildEnvironment(MessagingBroker.kafka, propertySources);
        final ArgumentCaptor<MapPropertySource> captor = ArgumentCaptor.forClass(MapPropertySource.class);

        //when
        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        //then
        verify(propertySources).addAfter(eq(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME), captor.capture());
        assertThat(captor.getValue().getProperty(SPRING_CLOUD_STREAM_DEFAULT_BINDER_KEY)).isEqualTo("kafka");

        assertThat(captor.getValue().getProperty(MANAGEMENT_HEALTH_RABBIT_ENABLED_KEY)).isEqualTo(false);
    }

    private ConfigurableEnvironment buildEnvironment(MessagingBroker broker, MutablePropertySources propertySources) {
        final ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        given(
            environment.getProperty(
                ActivitiMessagingEnvironmentPostProcessor.ACTIVITI_CLOUD_MESSAGING_BROKER_KEY,
                MessagingBroker.class,
                MessagingBroker.rabbitmq
            )
        )
            .willReturn(broker);
        given(environment.getPropertySources()).willReturn(propertySources);
        return environment;
    }
}
