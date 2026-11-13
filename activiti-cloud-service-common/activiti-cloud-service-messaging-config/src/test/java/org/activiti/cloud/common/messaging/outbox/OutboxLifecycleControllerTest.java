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

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.context.NamedComponent;

class OutboxLifecycleControllerTest {

    private SmartLifecycleRoleController roleController;
    private GenericApplicationContext applicationContext;
    private OutboxLifecycleController controller;

    @BeforeEach
    void setUp() {
        roleController = new SmartLifecycleRoleController();
        applicationContext = new GenericApplicationContext();
        applicationContext.refresh();
        controller = new OutboxLifecycleController(roleController, applicationContext);
    }

    @Test
    void shouldStartAndStopAllLifecyclesInOutboxRole() {
        TestSmartLifecycle endpoint1 = registerLifecycle("outboxEndpoint1", false);
        TestSmartLifecycle endpoint2 = registerLifecycle("outboxEndpoint2", false);

        controller.start();

        assertThat(endpoint1.isRunning()).isTrue();
        assertThat(endpoint2.isRunning()).isTrue();
        assertThat(controller.allEndpointsRunning()).isTrue();

        controller.stop();

        assertThat(endpoint1.isRunning()).isFalse();
        assertThat(endpoint2.isRunning()).isFalse();
        assertThat(controller.allEndpointsRunning()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("endpointStatesProvider")
    void shouldReportRunningStatusAndEndpointNames(
        Map<String, Boolean> initialStates,
        boolean expectedAllRunning,
        List<String> expectedEndpoints
    ) {
        initialStates.forEach(this::registerLifecycle);

        assertThat(controller.getEndpointsRunningStatus()).containsExactlyInAnyOrderEntriesOf(initialStates);
        assertThat(controller.allEndpointsRunning()).isEqualTo(expectedAllRunning);
        assertThat(controller.getEndpoints()).containsExactlyInAnyOrderElementsOf(expectedEndpoints);
    }

    @ParameterizedTest
    @ValueSource(strings = { "outboxChannelA", "outboxChannelB" })
    void shouldStartSpecificEndpointByBeanName(String endpointName) {
        TestSmartLifecycle target = registerLifecycle(endpointName, false);
        TestSmartLifecycle other = registerLifecycle("otherChannel", false);

        controller.start(endpointName);

        assertThat(target.isRunning()).isTrue();
        assertThat(other.isRunning()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outboxChannelA", "outboxChannelB" })
    void shouldStopSpecificEndpointByBeanName(String endpointName) {
        TestSmartLifecycle target = registerLifecycle(endpointName, true);
        TestSmartLifecycle other = registerLifecycle("otherChannel", true);

        controller.stop(endpointName);

        assertThat(target.isRunning()).isFalse();
        assertThat(other.isRunning()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "unknownEndpoint", "   " })
    void shouldSafelyIgnoreNullOrUnregisteredEndpoints(String invalidEndpoint) {
        SmartLifecycle spyLifecycle = mock(SmartLifecycle.class);
        applicationContext.registerBean("validEndpoint", SmartLifecycle.class, () -> spyLifecycle);
        roleController.addLifecycleToRole(OUTBOX_ROLE, spyLifecycle);

        controller.start(invalidEndpoint);
        controller.stop(invalidEndpoint);

        verify(spyLifecycle, org.mockito.Mockito.never()).start();
        verify(spyLifecycle, org.mockito.Mockito.never()).stop();
    }

    private TestSmartLifecycle registerLifecycle(String name, boolean initiallyRunning) {
        TestSmartLifecycle lifecycle = new TestSmartLifecycle(initiallyRunning).withComponentName(name);
        applicationContext.registerBean(name, SmartLifecycle.class, () -> lifecycle);
        roleController.addLifecycleToRole(OUTBOX_ROLE, lifecycle);
        return lifecycle;
    }

    static Stream<Arguments> endpointStatesProvider() {
        return Stream.of(
            Arguments.of(Map.of(), false, List.of()),
            Arguments.of(Map.of("endpoint1", true), true, List.of("endpoint1")),
            Arguments.of(Map.of("endpoint1", false), false, List.of("endpoint1")),
            Arguments.of(Map.of("endpoint1", true, "endpoint2", false), false, List.of("endpoint1", "endpoint2")),
            Arguments.of(Map.of("endpoint1", true, "endpoint2", true), true, List.of("endpoint1", "endpoint2"))
        );
    }

    private static class TestSmartLifecycle implements SmartLifecycle, NamedComponent {

        private final AtomicBoolean running;
        private String componentName;

        TestSmartLifecycle(boolean initiallyRunning) {
            this.running = new AtomicBoolean(initiallyRunning);
        }

        public TestSmartLifecycle withComponentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        @Override
        public void start() {
            running.set(true);
        }

        @Override
        public void stop() {
            running.set(false);
        }

        @Override
        public boolean isRunning() {
            return running.get();
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public String getComponentType() {
            return "test";
        }
    }
}
