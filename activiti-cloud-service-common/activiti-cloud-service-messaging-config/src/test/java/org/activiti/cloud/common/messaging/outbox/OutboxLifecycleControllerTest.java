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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.SmartLifecycleRoleController;

@ExtendWith(MockitoExtension.class)
class OutboxLifecycleControllerTest {

    @Mock
    private SmartLifecycleRoleController smartLifecycleRoleController;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private OutboxLifecycleController controller;

    @Test
    void shouldStartAllLifecyclesInOutboxRole() {
        controller.start();

        verify(smartLifecycleRoleController).startLifecyclesInRole(OUTBOX_ROLE);
    }

    @Test
    void shouldStopAllLifecyclesInOutboxRole() {
        controller.stop();

        verify(smartLifecycleRoleController).stopLifecyclesInRole(OUTBOX_ROLE);
    }

    @Test
    void shouldReturnEndpointsRunningStatus() {
        Map<String, Boolean> expectedStatus = Map.of("outboxEndpoint1", true, "outboxEndpoint2", false);
        when(smartLifecycleRoleController.getEndpointsRunningStatus(OUTBOX_ROLE)).thenReturn(expectedStatus);

        Map<String, Boolean> result = controller.getEndpointsRunningStatus();

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedStatus);
    }

    @Test
    void shouldReturnTrueWhenAllEndpointsAreRunning() {
        when(smartLifecycleRoleController.allEndpointsRunning(OUTBOX_ROLE)).thenReturn(true);

        boolean result = controller.allEndpointsRunning();

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotAllEndpointsAreRunning() {
        when(smartLifecycleRoleController.allEndpointsRunning(OUTBOX_ROLE)).thenReturn(false);

        boolean result = controller.allEndpointsRunning();

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnListOfEndpointNames() {
        Map<String, Boolean> statusMap = new LinkedHashMap<>();
        statusMap.put("endpointA", true);
        statusMap.put("endpointB", false);
        when(smartLifecycleRoleController.getEndpointsRunningStatus(OUTBOX_ROLE)).thenReturn(statusMap);

        List<String> endpoints = controller.getEndpoints();

        assertThat(endpoints).containsExactly("endpointA", "endpointB");
    }

    @Test
    void shouldStartSpecificEndpointWhenBeanExists() {
        String endpointName = "customOutboxEndpoint";
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenReturn(lifecycle);

        controller.start(endpointName);

        verify(lifecycle).start();
    }

    @Test
    void shouldNotFailWhenStartingNonExistentEndpoint() {
        String endpointName = "unknownEndpoint";
        when(applicationContext.containsBean(endpointName)).thenReturn(false);

        controller.start(endpointName);

        verify(applicationContext, never()).getBean(anyString(), any(Class.class));
    }

    @Test
    void shouldNotFailWhenStartingNullEndpoint() {
        controller.start(null);

        verifyNoInteractions(applicationContext);
    }

    @Test
    void shouldStopSpecificEndpointWhenBeanExists() {
        String endpointName = "customOutboxEndpoint";
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenReturn(lifecycle);

        controller.stop(endpointName);

        verify(lifecycle).stop();
    }

    @Test
    void shouldNotFailWhenStoppingNonExistentEndpoint() {
        String endpointName = "unknownEndpoint";
        when(applicationContext.containsBean(endpointName)).thenReturn(false);

        controller.stop(endpointName);

        verify(applicationContext, never()).getBean(anyString(), any(Class.class));
    }

    @Test
    void shouldNotFailWhenStoppingNullEndpoint() {
        controller.stop(null);

        verifyNoInteractions(applicationContext);
    }

    @Test
    void shouldThrowExceptionWhenStartingBeanNotOfSmartLifecycleType() {
        String endpointName = "notALifecycleBean";
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenThrow(
            new BeanNotOfRequiredTypeException(endpointName, SmartLifecycle.class, String.class)
        );

        assertThatExceptionOfType(BeanNotOfRequiredTypeException.class)
            .isThrownBy(() -> controller.start(endpointName))
            .withMessageContaining(endpointName);
    }

    @Test
    void shouldThrowExceptionWhenStoppingBeanNotOfSmartLifecycleType() {
        String endpointName = "notALifecycleBean";
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenThrow(
            new BeanNotOfRequiredTypeException(endpointName, SmartLifecycle.class, String.class)
        );

        assertThatExceptionOfType(BeanNotOfRequiredTypeException.class)
            .isThrownBy(() -> controller.stop(endpointName))
            .withMessageContaining(endpointName);
    }

    @Test
    void shouldPropagateExceptionWhenBeanRemovedConcurrentlyDuringStart() {
        String endpointName = "transientEndpoint";
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenThrow(
            new NoSuchBeanDefinitionException(endpointName)
        );

        assertThatExceptionOfType(BeansException.class)
            .isThrownBy(() -> controller.start(endpointName))
            .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void shouldPropagateRuntimeExceptionWhenEndpointStartFails() {
        String endpointName = "failingEndpoint";
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenReturn(lifecycle);
        doThrow(new IllegalStateException("Failed to start endpoint")).when(lifecycle).start();

        assertThatThrownBy(() -> controller.start(endpointName))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to start endpoint");
    }

    @Test
    void shouldPropagateRuntimeExceptionWhenEndpointStopFails() {
        String endpointName = "failingEndpoint";
        SmartLifecycle lifecycle = mock(SmartLifecycle.class);
        when(applicationContext.containsBean(endpointName)).thenReturn(true);
        when(applicationContext.getBean(endpointName, SmartLifecycle.class)).thenReturn(lifecycle);
        doThrow(new IllegalStateException("Failed to stop endpoint")).when(lifecycle).stop();

        assertThatThrownBy(() -> controller.stop(endpointName))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to stop endpoint");
    }

    @Test
    void shouldPropagateExceptionWhenRoleControllerFailsToStartAll() {
        doThrow(new RuntimeException("Role start failed"))
            .when(smartLifecycleRoleController)
            .startLifecyclesInRole(OUTBOX_ROLE);

        assertThatThrownBy(() -> controller.start())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Role start failed");
    }
}
