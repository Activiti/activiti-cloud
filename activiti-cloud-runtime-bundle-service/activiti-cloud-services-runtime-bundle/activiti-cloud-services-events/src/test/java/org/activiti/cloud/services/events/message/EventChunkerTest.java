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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties.RuntimeBundleEventsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventChunkerTest {

    private EventChunker eventChunker;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RuntimeBundleProperties runtimeBundleProperties = new RuntimeBundleProperties();
        runtimeBundleProperties.setEventsProperties(new RuntimeBundleEventsProperties());
        runtimeBundleProperties.getEventsProperties().setChunkSizeInBytesCloseListener(5000);
        eventChunker = new EventChunker(objectMapper, runtimeBundleProperties);
    }

    @Test
    void shouldReturnEmptyCollectionWhenEventsListIsEmpty() {
        List<CloudRuntimeEventImpl<?, ?>> events = Collections.emptyList();

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkWhenAllEventsAreSmallerThanMaxSize() {
        List<CloudRuntimeEventImpl<?, ?>> events = createSmallEvents(3);

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).hasSize(3);
    }

    @Test
    void shouldCreateMultipleChunksWhenTotalSizeExceedsLimit() {
        List<CloudRuntimeEventImpl<?, ?>> events = createLargeEvents(5);

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).hasSize(5);

        for (List<CloudRuntimeEventImpl<?, ?>> chunk : result) {
            assertThat(chunk).isNotEmpty();
            if (chunk.size() > 1) {
                int totalChunkSize = chunk.stream().mapToInt(this::getEventSize).sum();
                assertThat(totalChunkSize).isLessThanOrEqualTo(3000);
            }
        }
    }

    @Test
    void shouldHandleSingleEventLargerThanMaxSize() {
        List<CloudRuntimeEventImpl<?, ?>> events = createVeryLargeEvents(1);

        var exception = assertThrows(IllegalArgumentException.class, () -> eventChunker.chunk(events));
        assertThat(exception).hasMessage("Chunk size limit exceeded");
    }

    @Test
    void shouldCreateSeparateChunkForEachLargeEvent() {
        List<CloudRuntimeEventImpl<?, ?>> events = createLargeEvents(3);

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).hasSize(3);

        for (List<CloudRuntimeEventImpl<?, ?>> chunk : result) {
            assertThat(chunk).hasSize(1);
        }
    }

    @Test
    void shouldHandleMixedSizeEvents() {
        List<CloudRuntimeEventImpl<?, ?>> events = new ArrayList<>();
        events.addAll(createSmallEvents(2));
        events.addAll(createLargeEvents(1));
        events.addAll(createSmallEvents(2));

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).hasSize(3);

        int totalEvents = result.stream().mapToInt(List::size).sum();
        assertThat(totalEvents).isEqualTo(5);
    }

    @Test
    void shouldThrowExceptionWhenObjectMapperFails() throws JsonProcessingException {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        EventChunker chunkerWithMockMapper = new EventChunker(mockMapper, null);

        when(mockMapper.writeValueAsBytes(any())).thenThrow(JsonProcessingException.class);

        List<CloudRuntimeEventImpl<?, ?>> events = createSmallEvents(1);

        assertThatThrownBy(() -> chunkerWithMockMapper.chunk(events))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Failed to serialize event to JSON");
    }

    @Test
    void shouldCreateNewChunkWhenAddingEventWouldExceedLimit() {
        List<CloudRuntimeEventImpl<?, ?>> events = createMediumSizeEvents(3);

        Collection<List<CloudRuntimeEventImpl<?, ?>>> result = eventChunker.chunk(events);

        assertThat(result).hasSize(3);

        for (List<CloudRuntimeEventImpl<?, ?>> chunk : result) {
            if (chunk.size() > 1) {
                int totalSize = chunk.stream().mapToInt(this::getEventSize).sum();
                assertThat(totalSize).isLessThanOrEqualTo(3000);
            }
        }
    }

    private List<CloudRuntimeEventImpl<?, ?>> createSmallEvents(int count) {
        return createEvents(count, 100);
    }

    private List<CloudRuntimeEventImpl<?, ?>> createLargeEvents(int count) {
        return createEvents(count, 1500);
    }

    private List<CloudRuntimeEventImpl<?, ?>> createVeryLargeEvents(int count) {
        return createEvents(count, 5000);
    }

    private List<CloudRuntimeEventImpl<?, ?>> createMediumSizeEvents(int count) {
        return createEvents(count, 1000);
    }

    private List<CloudRuntimeEventImpl<?, ?>> createEvents(int count, int targetSizeBytes) {
        List<CloudRuntimeEventImpl<?, ?>> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProcessInstanceImpl processInstance = new ProcessInstanceImpl();

            int baseOverhead = 300;
            int dataNeeded = Math.max(0, targetSizeBytes - baseOverhead);

            StringBuilder data = new StringBuilder("EVENT_" + i + "_");
            String pattern = "data_pattern_for_size_testing_";

            while (data.length() < dataNeeded) {
                data.append(pattern);
            }

            processInstance.setId(data.toString());
            processInstance.setBusinessKey("business_key_" + i + "_" + data.substring(0, Math.min(50, data.length())));
            events.add(new CloudProcessCreatedEventImpl(processInstance));
        }
        return events;
    }

    private int getEventSize(CloudRuntimeEventImpl<?, ?> event) {
        try {
            return objectMapper.writeValueAsBytes(event).length;
        } catch (Exception e) {
            return 0;
        }
    }
}
