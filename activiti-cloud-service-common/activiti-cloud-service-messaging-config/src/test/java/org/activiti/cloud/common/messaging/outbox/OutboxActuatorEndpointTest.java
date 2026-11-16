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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxActuatorEndpointTest {

    @Mock
    private OutboxLifecycleController outboxLifecycleController;

    @InjectMocks
    private OutboxActuatorEndpoint endpoint;

    @Test
    void shouldReturnEndpointsRunningStatus() {
        // Given
        Map<String, Boolean> expectedStatus = Map.of("ordersOutbox", true, "paymentsOutbox", false);
        when(outboxLifecycleController.getEndpointsRunningStatus()).thenReturn(expectedStatus);

        // When
        Map<String, Boolean> actualStatus = endpoint.getEndpointsRunningStatus();

        // Then
        assertThat(actualStatus).containsExactlyInAnyOrderEntriesOf(expectedStatus);
        verify(outboxLifecycleController).getEndpointsRunningStatus();
        verifyNoMoreInteractions(outboxLifecycleController);
    }

    @Test
    void shouldStartOutboxLifecycleControllerWhenCommandIsStart() {
        // When
        endpoint.smartLifecycleRoleController("start");

        // Then
        verify(outboxLifecycleController).start();
        verifyNoMoreInteractions(outboxLifecycleController);
    }

    @Test
    void shouldStopOutboxLifecycleControllerWhenCommandIsStop() {
        // When
        endpoint.smartLifecycleRoleController("stop");

        // Then
        verify(outboxLifecycleController).stop();
        verifyNoMoreInteractions(outboxLifecycleController);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "restart", "START", "STOP", "unknown" })
    void shouldThrowIllegalArgumentExceptionWhenCommandIsInvalid(String invalidCommand) {
        // When / Then
        assertThatThrownBy(() -> endpoint.smartLifecycleRoleController(invalidCommand))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid command argument");

        verifyNoMoreInteractions(outboxLifecycleController);
    }
}
