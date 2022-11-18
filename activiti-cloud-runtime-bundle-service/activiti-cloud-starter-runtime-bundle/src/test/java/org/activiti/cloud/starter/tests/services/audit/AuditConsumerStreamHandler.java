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

import java.util.*;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Headers;

@Profile(AUDIT_PRODUCER_IT)
@TestComponent
@EnableBinding(AuditConsumer.class)
public class AuditConsumerStreamHandler {

    private volatile Map<String, Object> receivedHeaders = new HashMap<>();

    private volatile List<CloudRuntimeEvent<?, ?>> latestReceivedEvents = new ArrayList<>();
    private volatile List<CloudRuntimeEvent<?, ?>> allReceivedEvents = new ArrayList<>();

    @StreamListener(AuditConsumer.AUDIT_CONSUMER)
    public void receive(@Headers Map<String, Object> headers, CloudRuntimeEvent<?, ?>... events) {
        latestReceivedEvents = new ArrayList<>(Arrays.asList(events));
        allReceivedEvents = new ArrayList<>(allReceivedEvents);
        allReceivedEvents.addAll(latestReceivedEvents);
        receivedHeaders = new LinkedHashMap<>(headers);
    }

    public List<CloudRuntimeEvent<?, ?>> getLatestReceivedEvents() {
        return this.latestReceivedEvents;
    }

    public List<CloudRuntimeEvent<?, ?>> getAllReceivedEvents() {
        return allReceivedEvents;
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
