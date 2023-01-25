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
package org.activiti.cloud.connectors.starter.config;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.activiti.cloud.connectors.starter.channels.ProcessRuntimeChannels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

@Configuration
public class ProcessRuntimeChannelsConfiguration implements ProcessRuntimeChannels {

    @OutputBinding(RUNTIME_CMD_PRODUCER)
    @ConditionalOnMissingBean(name = RUNTIME_CMD_PRODUCER)
    @Override
    public MessageChannel runtimeCmdProducer() {
        return MessageChannels.direct(RUNTIME_CMD_PRODUCER).get();
    }

    @InputBinding(RUNTIME_CMD_RESULTS)
    @ConditionalOnMissingBean(name = RUNTIME_CMD_RESULTS)
    @Override
    public SubscribableChannel runtimeCmdResults() {
        return MessageChannels.publishSubscribe(RUNTIME_CMD_RESULTS).get();
    }
}
