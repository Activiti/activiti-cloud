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

package org.activiti.cloud.starter.rb.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.activiti.cloud.starter.rb.configuration.ActivitiAuditProducerPartitionKeyExtractor.ACTIVITI_AUDIT_PRODUCER_PATITION_KEY_EXTRACTOR_NAME;
import static org.activiti.cloud.starter.rb.configuration.ActivitiAuditProducerPartitionKeyExtractor.ACTIVITI_CLOUD_MESSAGING_PARTITIONED;
import static org.activiti.cloud.starter.rb.configuration.ActivitiAuditProducerPartitionKeyExtractor.ACTIVITI_CLOUD_MESSAGING_PARTITION_COUNT;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

@Order(Ordered.LOWEST_PRECEDENCE)
public class ActivitiAuditProducerEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ActivitiAuditProducerEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Optional<Boolean> isPartitioned = Optional.ofNullable(environment.getProperty(ACTIVITI_CLOUD_MESSAGING_PARTITIONED,
                                                                                      Boolean.class));

        logger.warn("Configuring " + ACTIVITI_CLOUD_MESSAGING_PARTITIONED + "={}", isPartitioned);

        Integer partitionCount = environment.getProperty(ACTIVITI_CLOUD_MESSAGING_PARTITION_COUNT,
                                                         Integer.class);

        // enable partitioned producer conditionally based on configuration property
        isPartitioned.filter(Boolean.TRUE::equals)
                     .ifPresent(value -> {
                         Map<String, Object> properties = new LinkedHashMap<>();

                         properties.put("spring.cloud.stream.bindings.auditProducer-out-0.producer.partitionKeyExtractorName",
                                        ACTIVITI_AUDIT_PRODUCER_PATITION_KEY_EXTRACTOR_NAME);
                         properties.put("spring.cloud.stream.bindings.auditProducer-out-0.producer.partitionCount",
                                        partitionCount);

                         environment.getPropertySources()
                                    .addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                              new MapPropertySource(ActivitiAuditProducerEnvironmentPostProcessor.class.getSimpleName(),
                                                                    properties));
                     });
    }
}
