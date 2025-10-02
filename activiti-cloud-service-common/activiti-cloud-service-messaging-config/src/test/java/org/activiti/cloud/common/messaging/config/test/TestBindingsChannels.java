/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.config.test;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface TestBindingsChannels {
    String COMMAND_CONSUMER = "commandConsumer";

    String AUDIT_CONSUMER = "auditConsumer";

    String QUERY_CONSUMER = "queryConsumer";

    String COMMAND_RESULTS = "commandResults";

    String AUDIT_PRODUCER = "auditProducer";

    String INTEGRATION_REQUESTS = "integrationRequests";

    String INTEGRATION_RESULTS = "integrationResults";

    String SCRIPT_RUNTIME_CONSUMER = "scriptRuntimeConsumer";

    String ENGINE_EVENTS_CONSUMER = "engineEventsConsumer";

    @InputBinding(value = COMMAND_CONSUMER)
    default SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(COMMAND_CONSUMER).getObject();
    }

    @InputBinding(value = QUERY_CONSUMER)
    default SubscribableChannel queryConsumer() {
        return MessageChannels.publishSubscribe(QUERY_CONSUMER).getObject();
    }

    @InputBinding(value = AUDIT_CONSUMER)
    default SubscribableChannel auditConsumer() {
        return MessageChannels.publishSubscribe(AUDIT_CONSUMER).getObject();
    }

    @OutputBinding(COMMAND_RESULTS)
    default MessageChannel commandResults() {
        return MessageChannels.direct(COMMAND_RESULTS).getObject();
    }

    @OutputBinding(AUDIT_PRODUCER)
    default MessageChannel auditProducer() {
        return MessageChannels.direct(AUDIT_PRODUCER).getObject();
    }

    @InputBinding(value = INTEGRATION_REQUESTS)
    default SubscribableChannel integrationRequests() {
        return MessageChannels.publishSubscribe(INTEGRATION_REQUESTS).getObject();
    }

    @OutputBinding(INTEGRATION_RESULTS)
    default MessageChannel integrationResults() {
        return MessageChannels.direct(INTEGRATION_RESULTS).getObject();
    }

    @InputBinding(value = SCRIPT_RUNTIME_CONSUMER)
    default SubscribableChannel scriptRuntimeConsumer() {
        return MessageChannels.publishSubscribe(SCRIPT_RUNTIME_CONSUMER).getObject();
    }

    @InputBinding(value = ENGINE_EVENTS_CONSUMER)
    default SubscribableChannel engineEventsConsumer() {
        return MessageChannels.publishSubscribe(ENGINE_EVENTS_CONSUMER).getObject();
    }
}
