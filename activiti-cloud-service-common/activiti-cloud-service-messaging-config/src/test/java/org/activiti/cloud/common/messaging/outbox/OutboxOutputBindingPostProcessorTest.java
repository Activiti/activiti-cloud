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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;
import org.activiti.cloud.common.messaging.config.FunctionAnnotationService;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.support.AopUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@ExtendWith(MockitoExtension.class)
class OutboxOutputBindingPostProcessorTest {

    @Mock
    private FunctionAnnotationService functionAnnotationService;

    @Mock
    private OutboxChannelInterceptorFactory outboxChannelInterceptorFactory;

    @Mock
    private BiFunction<Message<?>, MessageChannel, String> outboxDestinationProvider;

    @Mock
    private OutboxChannelConfigurer outboxChannelConfigurer;

    private OutboxOutputBindingPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        postProcessor = new OutboxOutputBindingPostProcessor(
            functionAnnotationService,
            outboxChannelInterceptorFactory,
            outboxDestinationProvider,
            outboxChannelConfigurer
        );
    }

    @Test
    void shouldReturnOriginalDirectChannelWhenOutputBindingAnnotationIsMissing() {
        // Given
        String beanName = "unannotatedChannel";
        DirectChannel channel = new DirectChannel();

        when(functionAnnotationService.findAnnotationOnBean(beanName, OutputBinding.class)).thenReturn(null);

        // When
        Object result = postProcessor.postProcessAfterInitialization(channel, beanName);

        // Then
        assertThat(result).isSameAs(channel);
        assertThat(channel.getInterceptors()).isEmpty();
        assertThat(AopUtils.isAopProxy(result)).isFalse();
        verifyNoInteractions(outboxChannelInterceptorFactory);
    }

    @Test
    void shouldReturnOriginalBeanUnchangedWhenNotDirectChannel() {
        // Given
        String beanName = "otherBean";
        Object nonChannelBean = new Object();

        // When
        Object result = postProcessor.postProcessAfterInitialization(nonChannelBean, beanName);

        // Then
        assertThat(result).isSameAs(nonChannelBean);
        assertThat(AopUtils.isAopProxy(result)).isFalse();
        verifyNoInteractions(functionAnnotationService, outboxChannelInterceptorFactory);
    }
}
