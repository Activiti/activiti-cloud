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
package org.activiti.cloud.services.audit.jpa.streams;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class AuditPayloadCleanupBatchTest {

    private final EventsRepository repository = mock(EventsRepository.class);
    private final APIEventToEntityConverters converters = mock(APIEventToEntityConverters.class);
    private final EventToEntityConverter converter = mock(EventToEntityConverter.class);
    private final AuditPayloadCleanup strategy = mock(AuditPayloadCleanup.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

    @Test
    void should_saveBatchBeforeInvokingStrategy() {
        beans.registerSingleton("cleanup", strategy);
        var entity = prepareEvent();

        handler().receiveCloudRuntimeEvent(Map.of("id", "batch"), new CloudProcessCompletedEventImpl());

        var order = inOrder(repository, strategy);
        order.verify(repository).saveAll(List.of(entity));
        order.verify(strategy).cleanup(List.of(entity));
    }

    @Test
    void should_saveWithoutCleanup_whenNoStrategyIsConfigured() {
        var entity = prepareEvent();

        handler().receiveCloudRuntimeEvent(Map.of("id", "batch"), new CloudProcessCompletedEventImpl());

        then(repository).should().saveAll(List.of(entity));
        then(strategy).shouldHaveNoInteractions();
    }

    @Test
    void should_notCleanup_whenSavingFails() {
        beans.registerSingleton("cleanup", strategy);
        prepareEvent();
        given(repository.saveAll(any())).willThrow(new IllegalStateException("save failed"));

        var handler = handler();
        Map<String, Object> id = Map.of("id", "batch");
        var cloudProcessCompletedEvent = new CloudProcessCompletedEventImpl();
        assertThatThrownBy(() -> handler.receiveCloudRuntimeEvent(id, cloudProcessCompletedEvent)).isInstanceOf(
            IllegalStateException.class
        );

        then(strategy).shouldHaveNoInteractions();
    }

    @Test
    void should_propagateCleanupFailure() {
        beans.registerSingleton("cleanup", strategy);
        prepareEvent();
        willThrow(new IllegalStateException("cleanup failed")).given(strategy).cleanup(anyList());

        var handler = handler();
        Map<String, Object> id = Map.of("id", "batch");
        var cloudProcessCompletedEvent = new CloudProcessCompletedEventImpl();
        assertThatThrownBy(() -> handler.receiveCloudRuntimeEvent(id, cloudProcessCompletedEvent))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("cleanup failed");
    }

    private ProcessCompletedEventEntity prepareEvent() {
        var entity = new ProcessCompletedEventEntity();
        given(converters.getConverterByEventTypeName("PROCESS_COMPLETED")).willReturn(converter);
        given(converter.convertToEntity(any())).willReturn(entity);
        return entity;
    }

    private AuditConsumerChannelHandlerImpl handler() {
        return new AuditConsumerChannelHandlerImpl(
            repository,
            converters,
            beans.getBeanProvider(AuditPayloadCleanup.class)
        );
    }
}
