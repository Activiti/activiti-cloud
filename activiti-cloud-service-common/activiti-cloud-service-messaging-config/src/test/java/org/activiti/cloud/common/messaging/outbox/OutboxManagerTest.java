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

package org.activiti.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;

@ExtendWith(MockitoExtension.class)
class OutboxManagerTest {

    @Mock
    private OutboxLifecycleController outboxLifecycleController;

    @Mock
    private OutboxLifecycleReconciler outboxLifecycleReconciler;

    private ConcurrentMetadataStore outboxMetadataStore;
    private OutboxManager outboxManager;

    enum LifecycleAction {
        START(OutboxManager::start, "true"),
        STOP(OutboxManager::stop, "false");

        final Consumer<OutboxManager> operation;
        final String expectedFlag;

        LifecycleAction(Consumer<OutboxManager> operation, String expectedFlag) {
            this.operation = operation;
            this.expectedFlag = expectedFlag;
        }
    }

    @BeforeEach
    void setUp() {
        outboxMetadataStore = new SimpleMetadataStore();
        outboxManager = new OutboxManager(outboxLifecycleController, outboxMetadataStore, outboxLifecycleReconciler);
    }

    @ParameterizedTest(name = "{0} with endpoints {1} sets metadata to {2}")
    @MethodSource("endpointScenarios")
    void operation_updatesMetadataForAllEndpointsAndReconciles(
        LifecycleAction action,
        List<String> endpoints,
        String expectedValue
    ) {
        String initialValue = expectedValue.equals("true") ? "false" : "true";
        endpoints.forEach(endpoint -> outboxMetadataStore.put(endpoint, initialValue));

        when(outboxLifecycleController.getEndpoints()).thenReturn(endpoints);

        action.operation.accept(outboxManager);

        assertThat(endpoints).allSatisfy(endpoint ->
            assertThat(outboxMetadataStore.get(endpoint)).isEqualTo(expectedValue)
        );
        verify(outboxLifecycleReconciler).reconcile();
    }

    static Stream<Arguments> endpointScenarios() {
        return Stream.of(
            Arguments.of(LifecycleAction.START, List.of(), "true"),
            Arguments.of(LifecycleAction.START, List.of("endpoint-1"), "true"),
            Arguments.of(LifecycleAction.START, List.of("endpoint-1", "endpoint-2"), "true"),
            Arguments.of(LifecycleAction.STOP, List.of(), "false"),
            Arguments.of(LifecycleAction.STOP, List.of("endpoint-1"), "false"),
            Arguments.of(LifecycleAction.STOP, List.of("endpoint-1", "endpoint-2"), "false")
        );
    }

    @ParameterizedTest
    @EnumSource(LifecycleAction.class)
    void operation_updatesMetadataStoreBeforeCallingReconcile(LifecycleAction action) {
        when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("endpoint-1", "endpoint-2"));

        doAnswer(invocation -> {
            assertThat(outboxMetadataStore.get("endpoint-1")).isEqualTo(action.expectedFlag);
            assertThat(outboxMetadataStore.get("endpoint-2")).isEqualTo(action.expectedFlag);
            return null;
        })
            .when(outboxLifecycleReconciler)
            .reconcile();

        action.operation.accept(outboxManager);

        verify(outboxLifecycleReconciler).reconcile();
    }

    @ParameterizedTest
    @EnumSource(LifecycleAction.class)
    void operation_whenMetadataStoreThrows_doesNotReconcile(LifecycleAction action) {
        ConcurrentMetadataStore failingStore = new SimpleMetadataStore() {
            @Override
            public void put(String key, String value) {
                throw new IllegalStateException("Store unavailable");
            }
        };
        OutboxManager managerWithFailingStore = new OutboxManager(
            outboxLifecycleController,
            failingStore,
            outboxLifecycleReconciler
        );
        when(outboxLifecycleController.getEndpoints()).thenReturn(List.of("endpoint-1"));

        assertThatThrownBy(() -> action.operation.accept(managerWithFailingStore))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Store unavailable");

        verifyNoInteractions(outboxLifecycleReconciler);
    }
}
