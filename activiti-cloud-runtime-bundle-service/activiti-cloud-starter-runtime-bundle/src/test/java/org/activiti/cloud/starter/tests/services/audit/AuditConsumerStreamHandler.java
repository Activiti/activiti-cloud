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
package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.cloud.starter.tests.services.audit.AuditProducerIT.AUDIT_PRODUCER_IT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

@Profile(AUDIT_PRODUCER_IT)
@TestConfiguration
@Import(AuditConsumerConfiguration.class)
public class AuditConsumerStreamHandler {

    private volatile Map<String, Object> receivedHeaders = new HashMap<>();

    private volatile List<CloudRuntimeEvent<?, ?>> latestReceivedEvents = new ArrayList<>();
    private volatile List<CloudRuntimeEvent<?, ?>> allReceivedEvents = new ArrayList<>();

    @FunctionBinding(input = AuditConsumer.AUDIT_CONSUMER)
    @Bean
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> receive() {
        return message -> {
            latestReceivedEvents = new ArrayList<>(message.getPayload());
            allReceivedEvents = new ArrayList<>(allReceivedEvents);
            allReceivedEvents.addAll(latestReceivedEvents);
            receivedHeaders = new LinkedHashMap<>(message.getHeaders());
        };
    }

    public List<CloudRuntimeEvent<?, ?>> getLatestReceivedEvents() {
        return this.latestReceivedEvents;
    }

    public List<CloudRuntimeEvent<?, ?>> getAllReceivedEvents() {
        return allReceivedEvents;
    }

    public <T extends CloudRuntimeEvent<?, ?>> List<T> getAllReceivedEvents(Class<T> eventType) {
        return allReceivedEvents
            .stream()
            .filter(eventType::isInstance)
            .map(eventType::cast)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getReceivedHeaders() {
        return receivedHeaders;
    }

    public void clear() {
        allReceivedEvents.clear();
        latestReceivedEvents.clear();
        receivedHeaders.clear();
    }
}
