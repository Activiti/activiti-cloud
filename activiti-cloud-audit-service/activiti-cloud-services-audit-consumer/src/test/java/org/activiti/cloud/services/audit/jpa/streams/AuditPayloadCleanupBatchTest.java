package org.activiti.cloud.services.audit.jpa.streams;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

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
