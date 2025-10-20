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
package org.activiti.cloud.services.events.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;

public class EventChunker {

    private final ObjectMapper objectMapper;
    private final RuntimeBundleProperties runtimeBundleProperties;

    public EventChunker(ObjectMapper objectMapper, RuntimeBundleProperties runtimeBundleProperties) {
        this.objectMapper = objectMapper;
        this.runtimeBundleProperties = runtimeBundleProperties;
    }

    public Collection<List<CloudRuntimeEventImpl<?, ?>>> chunk(List<CloudRuntimeEventImpl<?, ?>> events) {
        List<List<CloudRuntimeEventImpl<?, ?>>> chunks = new ArrayList<>();
        List<CloudRuntimeEventImpl<?, ?>> currentChunk = new ArrayList<>();
        var currentChunkSize = 0;

        for (CloudRuntimeEventImpl<?, ?> event : events) {
            var eventSizeInBytes = getEventSizeInBytes(event);

            if (
                eventSizeInBytes > this.runtimeBundleProperties.getEventsProperties().getChunkSizeInBytesCloseListener()
            ) {
                System.out.printf("Dropping event of size %d bytes (exceeds max chunk size bytes)%n", eventSizeInBytes);
            } else {
                if (isExceedingMaxLimit(currentChunkSize, eventSizeInBytes, currentChunk)) {
                    chunks.add(new ArrayList<>(currentChunk));
                    currentChunk.clear();
                    currentChunkSize = 0;

                    System.out.printf(
                        "Created chunk with %d events, starting new chunk due to size limit%n",
                        chunks.get(chunks.size() - 1).size()
                    );
                }

                currentChunk.add(event);
                currentChunkSize += eventSizeInBytes;

                System.out.printf(
                    "Added event of size %d bytes to chunk, current chunk size: %d bytes%n",
                    eventSizeInBytes,
                    currentChunkSize
                );
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        System.out.printf("Created %d chunks from %d events", chunks.size(), events.size());

        return chunks;
    }

    private boolean isExceedingMaxLimit(
        int currentChunkSize,
        int eventSizeInBytes,
        List<CloudRuntimeEventImpl<?, ?>> currentChunk
    ) {
        return (
            currentChunkSize +
            eventSizeInBytes >
            this.runtimeBundleProperties.getEventsProperties().getChunkSizeInBytesCloseListener() &&
            !currentChunk.isEmpty()
        );
    }

    private int getEventSizeInBytes(CloudRuntimeEventImpl<?, ?> event) {
        try {
            return this.objectMapper.writeValueAsBytes(event).length;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event to JSON", e);
        }
    }
}
