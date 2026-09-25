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
package org.activiti.cloud.services.query.app;

import java.util.List;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Feeds this instance's own {@code queryEvents} subscription into {@link RecomputeEventCapturer}. One
 * bad message is logged and skipped so it can't wedge the shared channel.
 */
public class PushedCountsQueryEventsConsumer implements Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushedCountsQueryEventsConsumer.class);

    private final RecomputeEventCapturer recomputeEventCapturer;

    public PushedCountsQueryEventsConsumer(RecomputeEventCapturer recomputeEventCapturer) {
        this.recomputeEventCapturer = recomputeEventCapturer;
    }

    @Override
    public void accept(Message<List<CloudRuntimeEvent<?, ?>>> message) {
        try {
            recomputeEventCapturer.capture(message.getPayload());
        } catch (RuntimeException e) {
            LOGGER.warn("Skipping a queryEvents batch that failed to capture for pushed counts", e);
        }
    }
}
