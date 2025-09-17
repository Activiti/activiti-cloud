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
package org.activiti.cloud.services.job.executor;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

public interface AsyncExecutorJobMessageChannels {
    @InputBinding(MessageBasedJobManagerChannelsConstants.INPUT)
    default SubscribableChannel asyncExecutorJobsInput() {
        return MessageChannels.publishSubscribe().getObject();
    }

    @OutputBinding(MessageBasedJobManagerChannelsConstants.OUTPUT)
    default DirectChannel asyncExecutorJobsOutput() {
        return MessageChannels.direct().getObject();
    }
}
