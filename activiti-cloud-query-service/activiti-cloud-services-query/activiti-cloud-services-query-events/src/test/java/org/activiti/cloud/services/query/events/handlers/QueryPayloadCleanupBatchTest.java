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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class QueryPayloadCleanupBatchTest {

    private final QueryEventHandlerContext handlers = mock(QueryEventHandlerContext.class);
    private final QueryEventHandlerContextOptimizer optimizer = mock(QueryEventHandlerContextOptimizer.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final QueryPayloadCleanup strategy = mock(QueryPayloadCleanup.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

    @BeforeEach
    void configureOptimizer() {
        given(optimizer.optimize(anyList())).willAnswer(call -> call.getArgument(0));
    }

    @Test
    void should_cleanupOriginalBatchOnce_afterAllChunksAreFlushed() {
        beans.registerSingleton("cleanup", strategy);
        var completed = new CloudProcessCompletedEventImpl();
        var created = new CloudProcessCreatedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(completed, created);
        var consumer = consumer().chunkSize(1);

        consumer.receive(events, Map.of());

        var order = inOrder(handlers, entityManager, strategy);
        order.verify(handlers).handle(created);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(handlers).handle(completed);
        order.verify(entityManager).flush();
        order.verify(entityManager).clear();
        order.verify(strategy).cleanup(same(events));
        then(strategy).shouldHaveNoMoreInteractions();
    }

    @Test
    void should_processBatch_whenNoCleanupStrategyIsConfigured() {
        var event = new CloudProcessCreatedEventImpl();

        consumer().receive(List.of(event), Map.of());

        then(handlers).should().handle(event);
        then(entityManager).should().flush();
    }

    @Test
    void should_notCleanup_whenEventHandlingFails() {
        beans.registerSingleton("cleanup", strategy);
        var event = new CloudProcessCreatedEventImpl();
        willThrow(new IllegalStateException("handler failed")).given(handlers).handle(event);

        var consumer = consumer();
        List<CloudRuntimeEvent<?, ?>> events = List.of(event);
        assertThatThrownBy(() -> consumer.receive(events, Map.of())).isInstanceOf(IllegalStateException.class);

        then(strategy).shouldHaveNoInteractions();
        then(entityManager).should().clear();
    }

    @Test
    void should_propagateCleanupFailure() {
        beans.registerSingleton("cleanup", strategy);
        willThrow(new IllegalStateException("cleanup failed")).given(strategy).cleanup(anyList());

        var consumer = consumer();
        List<CloudRuntimeEvent<?, ?>> events = List.of(new CloudProcessCreatedEventImpl());
        assertThatThrownBy(() -> consumer.receive(events, Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("cleanup failed");
    }

    private QueryConsumerChannelHandler consumer() {
        return new QueryConsumerChannelHandler(
            handlers,
            optimizer,
            entityManager,
            beans.getBeanProvider(QueryPayloadCleanup.class)
        );
    }
}
