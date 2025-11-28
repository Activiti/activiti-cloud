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
package org.activiti.cloud.services.query.events.handlers;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryEventHandlerContext {

    private static Logger LOGGER = LoggerFactory.getLogger(QueryEventHandlerContext.class);

    private final Map<String, QueryEventHandler> handlers;

    public QueryEventHandlerContext(Set<QueryEventHandler> handlers) {
        this.handlers =
            handlers.stream().collect(Collectors.toMap(QueryEventHandler::getHandledEvent, Function.identity()));
    }

    public void handle(CloudRuntimeEvent<?, ?>... events) {
        LOGGER.warn("[QUERY-TRACE] handle() called with {} events", events != null ? events.length : 0);
        if (events != null) {
            LOGGER.warn("[QUERY-TRACE] Starting to process {} events", events.length);
            Stream
                .of(events)
                .forEach(event -> {
                    LOGGER.warn(
                        "[QUERY-TRACE] Processing event: type={}, eventId={}, processInstanceId={}",
                        event.getEventType().name(),
                        event.getId(),
                        event.getProcessInstanceId()
                    );
                    QueryEventHandler handler = handlers.get(event.getEventType().name());
                    if (handler != null) {
                        LOGGER.warn("[QUERY-TRACE] Found handler for event type: {}", event.getEventType().name());
                        handler.handle(event);
                        LOGGER.warn("[QUERY-TRACE] Successfully handled event type: {}", event.getEventType().name());
                    } else {
                        LOGGER.warn(
                            "[QUERY-TRACE] No handler found for event: {} - ignoring",
                            event.getEventType().name()
                        );
                    }
                });
            LOGGER.warn("[QUERY-TRACE] Finished processing {} events", events.length);
        }
    }

    protected Map<String, QueryEventHandler> getHandlers() {
        return handlers;
    }
}
