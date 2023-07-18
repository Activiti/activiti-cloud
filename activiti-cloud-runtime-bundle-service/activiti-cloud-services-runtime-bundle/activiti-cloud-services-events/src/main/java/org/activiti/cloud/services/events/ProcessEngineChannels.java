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
package org.activiti.cloud.services.events;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessEngineChannels {
    String COMMAND_CONSUMER = "commandConsumer";

    String COMMAND_RESULTS = "commandResults";

    String AUDIT_PRODUCER = "auditProducer";

    @InputBinding(COMMAND_CONSUMER)
    default SubscribableChannel commandConsumer() {
        return MessageChannels.publishSubscribe(COMMAND_CONSUMER).getObject();
    }

    @OutputBinding(COMMAND_RESULTS)
    default MessageChannel commandResults() {
        return MessageChannels.direct(COMMAND_RESULTS).getObject();
    }

    @OutputBinding(AUDIT_PRODUCER)
    default MessageChannel auditProducer() {
        return MessageChannels.direct(AUDIT_PRODUCER).getObject();
    }
}
