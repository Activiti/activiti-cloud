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
package org.activiti.cloud.services.query.app.count;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Sends each recomputed count onto the {@code taskCountsProducer} binding as its own message.
 * <p>
 * One message per audience rather than one per batch, so that a relay can route on
 * {@link #SCOPE_KEY_HEADER} without opening the payload, and so that one unroutable audience does not
 * hold up the others.
 */
public class TaskCountsChannelPublisher implements TaskCountChangePublisher {

    /** Lets a relay filter by audience without deserializing the payload. */
    public static final String SCOPE_KEY_HEADER = "scopeKey";

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCountsChannelPublisher.class);

    private final MessageChannel taskCountsProducer;

    public TaskCountsChannelPublisher(MessageChannel taskCountsProducer) {
        this.taskCountsProducer = taskCountsProducer;
    }

    @Override
    public void publish(List<TaskCountChangedEvent> changes) {
        for (TaskCountChangedEvent change : changes) {
            try {
                taskCountsProducer.send(
                    MessageBuilder.withPayload(change).setHeader(SCOPE_KEY_HEADER, change.scopeKey()).build()
                );
            } catch (RuntimeException cause) {
                // The batch has already committed and the other audiences still deserve their counts, so
                // this failure is contained here. The affected client falls back to polling.
                LOGGER.error("Could not publish task count for scope {}", change.scopeKey(), cause);
            }
        }
    }
}
