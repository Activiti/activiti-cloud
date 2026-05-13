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

import com.rabbitmq.client.ConnectionFactory;
import java.util.Optional;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * Auto-configuration activated when {@code activiti.cloud.messaging.migration-mode=true}.
 *
 * <p>In migration mode the application:
 * <ol>
 *   <li>Blocks all {@link OutputBinding @OutputBinding} channels so no new messages are published
 *       to RabbitMQ. Any attempt to send a message is silently dropped and a warning is logged.
 *   <li>Leaves all input (consumer) bindings untouched so existing messages in RabbitMQ queues
 *       continue to be processed and drained.
 *   <li>Exposes a {@code rabbitMqDrain} {@link HealthIndicator} (available via
 *       {@code /actuator/health/rabbitMqDrain}) that reports:
 *       <ul>
 *         <li>{@code OUT_OF_SERVICE} – queues still contain pending messages.</li>
 *         <li>{@code UP} – all input-binding queues are empty; it is safe to migrate.</li>
 *       </ul>
 * </ol>
 *
 * <p>Enable via environment variable {@code ACT_MESSAGING_MIGRATION_MODE=true} or Spring
 * property {@code activiti.cloud.messaging.migration-mode=true}.
 */
@AutoConfiguration(after = ActivitiCloudMessagingAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX,
    name = "migration-mode",
    havingValue = "true"
)
public class MigrationModeMessagingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MigrationModeMessagingAutoConfiguration.class);

    @Bean
    ApplicationRunner migrationModeActivationRunner() {
        return args ->
            log.warn(
                "MIGRATION MODE is active: all output (producer) bindings are BLOCKED. " +
                "Input (consumer) bindings are still running to drain existing messages. " +
                "Monitor drain progress via the 'rabbitMqDrain' health indicator."
            );
    }

    /**
     * Registers a {@link BeanPostProcessor} that intercepts every {@link OutputBinding @OutputBinding}
     * {@link MessageChannel} and inserts a front-of-the-line {@link ChannelInterceptor} that drops
     * all outbound messages, effectively making producers a no-op during migration.
     */
    @Bean
    BeanPostProcessor migrationModeOutputBlockingPostProcessor(DefaultListableBeanFactory beanFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof InterceptableChannel channel) {
                    Optional
                        .ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class))
                        .ifPresent(annotation -> {
                            log.warn("Migration mode: blocking output channel '{}'", beanName);
                            channel.addInterceptor(0, new DroppingChannelInterceptor(beanName));
                        });
                }
                return bean;
            }
        };
    }

    private static final class DroppingChannelInterceptor implements ChannelInterceptor {

        private static final Logger log = LoggerFactory.getLogger(DroppingChannelInterceptor.class);

        private final String channelName;

        DroppingChannelInterceptor(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            log.warn(
                "Migration mode: dropping outbound message on channel '{}' — no messages will be produced during migration.",
                channelName
            );
            return null;
        }
    }

    /**
     * RabbitMQ-specific beans: only active when both the RabbitMQ client and Spring Boot
     * Actuator are on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ConnectionFactory.class, RabbitAdmin.class, HealthIndicator.class })
    static class RabbitMqDrainHealthConfiguration {

        @Bean
        RabbitMqDrainHealthIndicator rabbitMqDrainHealthIndicator(
            RabbitAdmin rabbitAdmin,
            BindingServiceProperties bindingServiceProperties
        ) {
            return new RabbitMqDrainHealthIndicator(rabbitAdmin, bindingServiceProperties);
        }
    }
}
