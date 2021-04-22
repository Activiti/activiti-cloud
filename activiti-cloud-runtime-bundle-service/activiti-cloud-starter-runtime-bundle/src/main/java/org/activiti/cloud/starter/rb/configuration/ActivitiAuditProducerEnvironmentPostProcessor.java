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
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

@Order(Ordered.LOWEST_PRECEDENCE)
public class ActivitiAuditProducerEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ActivitiAuditProducerEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        PropertySource<?> system = environment.getPropertySources()
                                              .get(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        // TODO enable partitioned producer conditionally based on system environment property
        if (true) {
            Map<String, Object> activitiAuditProducerProperties = new LinkedHashMap<>();

            activitiAuditProducerProperties.put("spring.cloud.stream.bindings.auditProducer.producer.partitionKeyExtractorName",
                                                "activitiAuditProducerPartitionKeyExtractor");
            activitiAuditProducerProperties.put("spring.cloud.stream.bindings.queryConsumer.consumer.partitioned",
                                                "true");
            activitiAuditProducerProperties.put("spring.cloud.stream.bindings.auditConsumer.consumer.partitioned",
                                                "true");

            environment.getPropertySources()
                       .addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                 new MapPropertySource("activitiAuditProducerPropertySource",
                                                       activitiAuditProducerProperties));
        }

    }
}
