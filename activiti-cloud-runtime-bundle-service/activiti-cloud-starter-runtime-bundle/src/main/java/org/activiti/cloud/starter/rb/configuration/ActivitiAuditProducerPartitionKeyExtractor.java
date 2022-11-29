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

import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.messaging.Message;

import java.util.Optional;
import java.util.UUID;

public class ActivitiAuditProducerPartitionKeyExtractor implements PartitionKeyExtractorStrategy {

    public static final String ACTIVITI_CLOUD_MESSAGING_PARTITION_COUNT = "activiti.cloud.messaging.partition-count";
    public static final String ACTIVITI_CLOUD_MESSAGING_PARTITIONED = "activiti.cloud.messaging.partitioned";
    public static final String ACTIVITI_AUDIT_PRODUCER_PATITION_KEY_EXTRACTOR_NAME = "activitiAuditProducerPartitionKeyExtractor";
    public static final String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";

    @Override
    public Object extractKey(Message<?> message) {
        // Use processInstanceId header to route message between partitions or use random hash value if missing
        String rootProcessInstance = message.getHeaders()
                                            .get(ROOT_PROCESS_INSTANCE_ID,
                                                 String.class);

        return Optional.ofNullable(rootProcessInstance)
                       .orElse(UUID.randomUUID().toString());
    }
}
