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
package org.activiti.cloud.services.gateway.channels;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessRuntimeGatewayChannels {
    String PROCESS_RUNTIME_GATEWAY_PRODUCER = "ProcessRuntimeGatewayProducer";
    String PROCESS_RUNTIME_GATEWAY_RESULTS = "ProcessRuntimeGatewayResults";

    @OutputBinding(PROCESS_RUNTIME_GATEWAY_PRODUCER)
    default MessageChannel processRuntimeGatewayProducer() {
        return MessageChannels.direct(PROCESS_RUNTIME_GATEWAY_PRODUCER).getObject();
    }

    @InputBinding(PROCESS_RUNTIME_GATEWAY_RESULTS)
    default SubscribableChannel processRuntimeGatewayResults() {
        return MessageChannels.publishSubscribe(PROCESS_RUNTIME_GATEWAY_RESULTS).getObject();
    }
}
