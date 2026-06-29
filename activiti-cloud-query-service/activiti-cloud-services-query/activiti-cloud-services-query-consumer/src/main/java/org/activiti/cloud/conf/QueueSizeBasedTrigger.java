/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.conf;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

public class QueueSizeBasedTrigger implements Trigger {

    private final QueueChannel queueChannel;
    private final Duration fastRate;
    private final Duration slowRate;

    public QueueSizeBasedTrigger(QueueChannel queueChannel, Duration slowRate, Duration fastRate) {
        this.queueChannel = queueChannel;
        this.slowRate = slowRate;
        this.fastRate = fastRate;
    }

    @Override
    public @Nullable Instant nextExecution(TriggerContext triggerContext) {
        Instant lastExecution = triggerContext.lastActualExecution();
        if (lastExecution == null) {
            return Instant.now(); // First execution
        }

        Duration delay = (this.queueChannel.getQueueSize() > 0) ? fastRate : slowRate;

        return lastExecution.plus(delay);
    }
}
