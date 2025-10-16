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

public class EventChunker {

    private static final int MAX_MESSAGE_SIZE_BYTES = 3000;

    private final ObjectMapper objectMapper;

    public EventChunker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Collection<List<CloudRuntimeEventImpl<?, ?>>> chunk(List<CloudRuntimeEventImpl<?, ?>> events) {
        List<List<CloudRuntimeEventImpl<?, ?>>> chunks = new ArrayList<>();
        List<CloudRuntimeEventImpl<?, ?>> currentChunk = new ArrayList<>();
        int currentChunkSize = 0;

        for (CloudRuntimeEventImpl<?, ?> event : events) {
            var eventSizeInBytes = getEventSizeInBytes(event);

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

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        System.out.printf(
            "Created %d chunks from %d events, max chunk size: %d bytes%n",
            chunks.size(),
            events.size(),
            MAX_MESSAGE_SIZE_BYTES
        );

        return chunks;
    }

    private boolean isExceedingMaxLimit(
        int currentChunkSize,
        int eventSizeInBytes,
        List<CloudRuntimeEventImpl<?, ?>> currentChunk
    ) {
        return currentChunkSize + eventSizeInBytes > MAX_MESSAGE_SIZE_BYTES && !currentChunk.isEmpty();
    }

    private int getEventSizeInBytes(CloudRuntimeEventImpl<?, ?> event) {
        try {
            return objectMapper.writeValueAsBytes(event).length;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event to JSON", e);
        }
    }
}
