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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.cloud.common.messaging.config.StreamBridgeOperations;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class OutboxStreamBridgeOperationsPostProcessorTest {

    private final OutboxStreamBridgeOperationsPostProcessor postProcessor =
        new OutboxStreamBridgeOperationsPostProcessor();

    @Test
    void shouldWrapStreamBridgeOperationsInCglibProxyWithOutboxAdvice() {
        // Given
        StreamBridgeOperations target = mock(StreamBridgeOperations.class);

        // When
        Object processedBean = postProcessor.postProcessAfterInitialization(target, "streamBridgeOperations");

        // Then
        assertThat(processedBean)
            .isNotNull()
            .isNotSameAs(target)
            .isInstanceOf(StreamBridgeOperations.class)
            .isInstanceOf(Advised.class);

        assertThat(AopUtils.isCglibProxy(processedBean)).isTrue();

        Advised advised = (Advised) processedBean;
        assertThat(advised.getAdvisors())
            .hasSize(1)
            .extracting(advisor -> advisor.getAdvice())
            .hasOnlyElementsOfType(OutboxStreamBridgeAdvice.class);
    }

    @Test
    void shouldInterceptSendCallsThroughCreatedProxy() {
        // Given
        StreamBridgeOperations target = mock(StreamBridgeOperations.class);
        Message<String> messageWithoutOutboxHeader = MessageBuilder.withPayload("payload").build();
        when(target.send("my-binding", messageWithoutOutboxHeader)).thenReturn(false);

        StreamBridgeOperations proxiedBean = (StreamBridgeOperations) postProcessor.postProcessAfterInitialization(
            target,
            "streamBridgeOperations"
        );

        // When
        boolean result = proxiedBean.send("my-binding", messageWithoutOutboxHeader);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnOriginalBeanUnchangedWhenNotStreamBridgeOperations() {
        // Given
        Object otherBean = new Object();

        // When
        Object processedBean = postProcessor.postProcessAfterInitialization(otherBean, "otherBean");

        // Then
        assertThat(processedBean).isSameAs(otherBean);
        assertThat(AopUtils.isAopProxy(processedBean)).isFalse();
    }
}
