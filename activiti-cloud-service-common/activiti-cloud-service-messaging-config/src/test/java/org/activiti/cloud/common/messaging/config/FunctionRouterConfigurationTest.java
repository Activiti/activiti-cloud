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
package org.activiti.cloud.common.messaging.config;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

class FunctionRouterConfigurationTest {

    private AnnotationConfigApplicationContext context;
    private BindingServiceProperties bindingServiceProperties;
    private BeanPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(ChannelConfiguration.class);
        bindingServiceProperties = new BindingServiceProperties();
        postProcessor = new FunctionRouterConfiguration().outputBindingChannelPostProcessor(
            new FunctionAnnotationService(context.getDefaultListableBeanFactory()),
            bindingServiceProperties
        );
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void shouldIgnoreGeneratedChannelBeforeRegistration() {
        DirectChannel channel = new DirectChannel();
        String beanName = "org.springframework.integration.dsl.StandardIntegrationFlow#0.subFlow#0.channel#0";

        assertThat(context.containsBean(beanName)).isFalse();
        assertThat(postProcessor.postProcessAfterInitialization(channel, beanName)).isSameAs(channel);
        assertThat(channel.getInterceptors()).isEmpty();
    }

    @Test
    void shouldIgnoreChannelWithoutOutputBinding() {
        DirectChannel channel = context.getBean("plainChannel", DirectChannel.class);

        assertThat(postProcessor.postProcessAfterInitialization(channel, "plainChannel")).isSameAs(channel);
        assertThat(channel.getInterceptors()).isEmpty();
    }

    @Test
    void shouldAddDestinationHeaderToOutputBindingChannel() {
        DirectChannel channel = context.getBean("outputChannel", DirectChannel.class);
        BindingProperties binding = new BindingProperties();
        binding.setDestination("engineEvents");
        bindingServiceProperties.getBindings().put("outputChannel", binding);
        Message<String> message = new GenericMessage<>("payload");

        postProcessor.postProcessAfterInitialization(channel, "outputChannel");

        assertThat(channel.getInterceptors()).hasSize(1);
        Message<?> intercepted = channel.getInterceptors().getFirst().preSend(message, channel);
        assertThat(intercepted).isNotNull();
        assertThat(intercepted.getPayload()).isEqualTo("payload");
        assertThat(intercepted.getHeaders()).containsEntry(FUNCTION_DESTINATION, "engineEvents");
    }

    @Test
    void shouldPreserveMessageWhenOutputBindingHasNoConfiguration() {
        DirectChannel channel = context.getBean("outputChannel", DirectChannel.class);
        Message<String> message = new GenericMessage<>("payload");

        postProcessor.postProcessAfterInitialization(channel, "outputChannel");

        assertThat(channel.getInterceptors()).hasSize(1);
        assertThat(channel.getInterceptors().getFirst().preSend(message, channel)).isSameAs(message);
    }

    @Configuration(proxyBeanMethods = false)
    static class ChannelConfiguration {

        @OutputBinding("outputChannel")
        DirectChannel outputChannel() {
            return new DirectChannel();
        }

        @Bean
        DirectChannel plainChannel() {
            return new DirectChannel();
        }
    }
}
