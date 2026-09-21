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
package org.activiti.cloud.services.events.message;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
        var currentChunkSize = 0L;

        for (CloudRuntimeEventImpl<?, ?> event : events) {
            var eventSizeInBytes = getEventSizeInBytes(event);

            if (isSingleEventExceedingMaxLimit(eventSizeInBytes)) {
                throw new IllegalArgumentException("Chunk size limit exceeded");
            }

            if (wouldChunkExceedMaxLimit(currentChunkSize, eventSizeInBytes, currentChunk)) {
                chunks.add(new ArrayList<>(currentChunk));
                currentChunk.clear();
                currentChunkSize = 0;
            }

            currentChunk.add(event);
            currentChunkSize += eventSizeInBytes;
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private boolean isSingleEventExceedingMaxLimit(long eventSizeInBytes) {
        return eventSizeInBytes > this.runtimeBundleProperties.getEventsProperties().getChunkSizeInBytesCloseListener();
    }

    private boolean wouldChunkExceedMaxLimit(
        long currentChunkSize,
        long eventSizeInBytes,
        List<CloudRuntimeEventImpl<?, ?>> currentChunk
    ) {
        return (
            currentChunkSize + eventSizeInBytes >
                this.runtimeBundleProperties.getEventsProperties().getChunkSizeInBytesCloseListener() &&
            !currentChunk.isEmpty()
        );
    }

    private long getEventSizeInBytes(CloudRuntimeEventImpl<?, ?> event) {
        try (var counter = new CountingOutputStream()) {
            this.objectMapper.writeValue(counter, event);
            return counter.getCount();
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize event to JSON", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Unexpected I/O error while counting event JSON size", e);
        }
    }

    private static class CountingOutputStream extends OutputStream {

        private long count = 0;

        @Override
        public void write(int b) {
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            count += len;
        }

        public long getCount() {
            return count;
        }
    }
}
