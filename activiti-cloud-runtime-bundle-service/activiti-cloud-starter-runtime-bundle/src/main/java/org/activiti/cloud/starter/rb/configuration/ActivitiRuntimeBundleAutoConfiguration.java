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

import static org.activiti.cloud.starter.rb.configuration.ActivitiAuditProducerPartitionKeyExtractor.ACTIVITI_AUDIT_PRODUCER_PATITION_KEY_EXTRACTOR_NAME;
import static org.activiti.cloud.starter.rb.configuration.ActivitiAuditProducerPartitionKeyExtractor.ACTIVITI_CLOUD_MESSAGING_PARTITIONED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RuntimeBundleSwaggerConfig.class)
public class ActivitiRuntimeBundleAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = ACTIVITI_CLOUD_MESSAGING_PARTITIONED, havingValue = "true")
    @ConditionalOnMissingBean(name = ACTIVITI_AUDIT_PRODUCER_PATITION_KEY_EXTRACTOR_NAME)
    public ActivitiAuditProducerPartitionKeyExtractor activitiAuditProducerPartitionKeyExtractor() {
        return new ActivitiAuditProducerPartitionKeyExtractor();
    }
}
