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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.util.StringUtils;

/**
 * A {@link HealthIndicator} that reports the drain status of RabbitMQ input queues during
 * migration mode ({@code activiti.cloud.messaging.migration-mode=true}).
 *
 * <ul>
 *   <li>Returns {@link org.springframework.boot.actuate.health.Status#OUT_OF_SERVICE} with per-queue
 *       pending-message counts while queues still hold messages.
 *   <li>Returns {@link org.springframework.boot.actuate.health.Status#UP} once every input-binding
 *       queue has been fully drained (zero pending messages).
 * </ul>
 *
 * Accessible via the {@code /actuator/health/rabbitMqDrain} endpoint.
 */
public class RabbitMqDrainHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqDrainHealthIndicator.class);

    static final String MIGRATION_MODE_KEY = "migrationMode";
    static final String DRAINING = "draining";
    static final String DRAIN_COMPLETE = "drain-complete";
    static final String TOTAL_PENDING_MESSAGES_KEY = "totalPendingMessages";

    private final RabbitAdmin rabbitAdmin;
    private final BindingServiceProperties bindingServiceProperties;

    public RabbitMqDrainHealthIndicator(
        RabbitAdmin rabbitAdmin,
        BindingServiceProperties bindingServiceProperties
    ) {
        this.rabbitAdmin = rabbitAdmin;
        this.bindingServiceProperties = bindingServiceProperties;
    }

    @Override
    public Health health() {
        String inputBindings = bindingServiceProperties.getInputBindings();

        if (!StringUtils.hasText(inputBindings)) {
            return Health.up().withDetail(MIGRATION_MODE_KEY, DRAIN_COMPLETE).withDetail("message", "No input bindings configured").build();
        }

        Map<String, Long> queueDepths = new LinkedHashMap<>();
        long totalMessages = 0;

        for (String bindingName : inputBindings.split(";")) {
            String trimmed = bindingName.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }

            BindingProperties binding = bindingServiceProperties.getBindingProperties(trimmed);
            String queueName = resolveQueueName(binding.getDestination(), binding.getGroup());
            long messageCount = queryMessageCount(queueName);
            queueDepths.put(queueName, messageCount);
            if (messageCount > 0) {
                totalMessages += messageCount;
            }
        }

        if (totalMessages == 0) {
            return Health.up().withDetail(MIGRATION_MODE_KEY, DRAIN_COMPLETE).withDetails(queueDepths).build();
        }

        return Health
            .outOfService()
            .withDetail(MIGRATION_MODE_KEY, DRAINING)
            .withDetail(TOTAL_PENDING_MESSAGES_KEY, totalMessages)
            .withDetails(queueDepths)
            .build();
    }

    private long queryMessageCount(String queueName) {
        try {
            Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props == null) {
                log.warn("Migration mode drain check: queue '{}' not found or inaccessible", queueName);
                return 0;
            }
            Object count = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            return count instanceof Number number ? number.longValue() : 0;
        } catch (Exception e) {
            log.warn("Migration mode drain check: failed to query queue '{}': {}", queueName, e.getMessage());
            return 0;
        }
    }

    private String resolveQueueName(String destination, String group) {
        if (StringUtils.hasText(group)) {
            return destination + "." + group;
        }
        return destination;
    }
}
