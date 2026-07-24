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

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties.ConnectorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Order
public class ConnectorOutputBindingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorOutputBindingEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME =
        ConnectorOutputBindingEnvironmentPostProcessor.class.getSimpleName();

    private static final String CONNECTORS_PREFIX =
        ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX + ".connectors";

    private static final String OUTPUT_BINDINGS_KEY = "spring.cloud.stream.output-bindings";

    private static final String OUTPUT_BINDINGS_SEPARATOR = ";";

    private static final String BINDING_DESTINATION_FORMAT = "spring.cloud.stream.bindings.[%s].destination";

    private static final String BINDING_REQUIRED_GROUPS_FORMAT =
        "spring.cloud.stream.bindings.[%s].producer.required-groups";

    private static final String RABBIT_QUEUE_NAME_GROUP_ONLY_FORMAT =
        "spring.cloud.stream.rabbit.bindings.[%s].producer.queue-name-group-only";

    private static final String RABBITMQ_PREFIX_PROPERTY =
        ActivitiCloudMessagingProperties.ACTIVITI_CLOUD_MESSAGING_PREFIX + ".rabbitmq.prefix";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        Map<String, ConnectorProperties> connectors = Binder.get(environment)
            .bind(
                CONNECTORS_PREFIX,
                Bindable.mapOf(String.class, ConnectorProperties.class),
                new ValidationBindHandler(validator)
            )
            .orElseGet(Collections::emptyMap);

        if (connectors.isEmpty()) {
            return;
        }

        String rabbitMqPrefix = environment.getProperty(RABBITMQ_PREFIX_PROPERTY, "");

        Map<String, Object> contributedProperties = new LinkedHashMap<>();
        Set<String> outputBindings = existingOutputBindings(environment);

        connectors.forEach((_, connectorProperties) -> {
            String bindingKey = connectorProperties.getBindingKey();

            outputBindings.add(bindingKey);

            String destination = connectorProperties.getDestination();

            if (!StringUtils.hasText(destination)) {
                destination = bindingKey;
            }

            contributedProperties.put(BINDING_DESTINATION_FORMAT.formatted(bindingKey), destination);

            Boolean queueNameGroupOnly = connectorProperties.isQueueNameGroupOnly();
            String[] requiredGroups = connectorProperties.getRequiredGroups();
            if (requiredGroups != null && requiredGroups.length > 0) {
                String[] groups = getRequiredGroups(requiredGroups, queueNameGroupOnly, rabbitMqPrefix);
                contributedProperties.put(
                    BINDING_REQUIRED_GROUPS_FORMAT.formatted(bindingKey),
                    String.join(",", groups)
                );
            }

            if (queueNameGroupOnly != null) {
                contributedProperties.put(
                    RABBIT_QUEUE_NAME_GROUP_ONLY_FORMAT.formatted(bindingKey),
                    queueNameGroupOnly.toString()
                );
            }

            LOG.info(
                "Pre-provisioning producer binding '{}' (destination='{}', required-groups={}, queue-name-group-only={}, prefix='{}')",
                bindingKey,
                destination,
                requiredGroups == null ? "[]" : String.join(",", requiredGroups),
                queueNameGroupOnly,
                rabbitMqPrefix
            );
        });

        contributedProperties.put(OUTPUT_BINDINGS_KEY, String.join(OUTPUT_BINDINGS_SEPARATOR, outputBindings));

        environment
            .getPropertySources()
            .addAfter(
                SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(PROPERTY_SOURCE_NAME, contributedProperties)
            );
    }

    private String[] getRequiredGroups(String[] requiredGroups, Boolean queueNameGroupOnly, String rabbitMqPrefix) {
        return Boolean.TRUE.equals(queueNameGroupOnly) && StringUtils.hasText(rabbitMqPrefix)
            ? applyPrefixToRequiredGroups(requiredGroups, rabbitMqPrefix)
            : requiredGroups;
    }

    private Set<String> existingOutputBindings(ConfigurableEnvironment environment) {
        Set<String> bindings = new LinkedHashSet<>();
        String existing = environment.getProperty(OUTPUT_BINDINGS_KEY);
        if (StringUtils.hasText(existing)) {
            for (String binding : existing.split(OUTPUT_BINDINGS_SEPARATOR)) {
                if (StringUtils.hasText(binding)) {
                    bindings.add(binding.trim());
                }
            }
        }
        return bindings;
    }

    private String[] applyPrefixToRequiredGroups(String[] groups, String prefix) {
        String[] prefixedGroups = new String[groups.length];
        for (int i = 0; i < groups.length; i++) {
            prefixedGroups[i] = prefix + groups[i];
        }
        return prefixedGroups;
    }
}
