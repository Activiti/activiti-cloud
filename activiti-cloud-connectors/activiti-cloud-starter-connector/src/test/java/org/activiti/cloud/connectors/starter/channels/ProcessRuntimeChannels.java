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
package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessRuntimeChannels {
    String RUNTIME_CMD_PRODUCER = "runtimeCmdProducer";
    String RUNTIME_CMD_RESULTS = "runtimeCmdResults";

    @OutputBinding(RUNTIME_CMD_PRODUCER)
    default MessageChannel runtimeCmdProducer() {
        return MessageChannels.direct(RUNTIME_CMD_PRODUCER).getObject();
    }

    @InputBinding(RUNTIME_CMD_RESULTS)
    default SubscribableChannel runtimeCmdResults() {
        return MessageChannels.publishSubscribe(RUNTIME_CMD_RESULTS).getObject();
    }
}
