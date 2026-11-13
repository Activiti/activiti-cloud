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

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptor.OUTBOX_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class OutboxChannelInterceptorAdviceTest {

    private static final String BEAN_NAME = "testOutboxChannel";

    @Mock
    private MethodInvocation invocation;

    private OutboxChannelInterceptorAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new OutboxChannelInterceptorAdvice(BEAN_NAME);
    }

    @ParameterizedTest(name = "proceedResult={0}, hasOutboxHeader={1} -> expectedResult={2}")
    @CsvSource({
        "false, false, true" /* 1 */,
        "false, true,  false" /* 2 */,
        "true,  false, true" /* 3 */,
        "true,  true,  true" /* 4 */,
    })
    void invoke_whenSendMethodCalled_returnsExpectedResult(
        boolean proceedResult,
        boolean hasOutboxHeader,
        boolean expectedResult
    ) throws Throwable {
        Message<String> message = createMessage(hasOutboxHeader);
        stubSendInvocation(message, proceedResult);

        Object result = advice.invoke(invocation);

        assertThat(result).isEqualTo(expectedResult);
        verify(invocation).proceed();
    }

    @Test
    void invoke_whenNonSendMethodCalled_proceedsDirectly() throws Throwable {
        stubMethodInvocation(Object.class.getMethod("toString"), "channel-representation");

        Object result = advice.invoke(invocation);

        assertThat(result).isEqualTo("channel-representation");
        verify(invocation).proceed();
    }

    @Test
    void invoke_whenProceedThrowsException_propagatesException() throws Throwable {
        Message<String> message = createMessage(false);
        stubSendInvocation(message);
        when(invocation.proceed()).thenThrow(new IllegalStateException("Channel closed"));

        assertThatThrownBy(() -> advice.invoke(invocation))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Channel closed");
    }

    // --- Helper Methods ---

    private Message<String> createMessage(boolean includeOutboxHeader) {
        MessageBuilder<String> builder = MessageBuilder.withPayload("test-payload");
        if (includeOutboxHeader) {
            builder.setHeader(OUTBOX_DESTINATION, "some-topic");
        }
        return builder.build();
    }

    private void stubSendInvocation(Message<?> message, boolean proceedResult) throws Throwable {
        stubSendInvocation(message);
        when(invocation.proceed()).thenReturn(proceedResult);
    }

    private void stubSendInvocation(Message<?> message) throws NoSuchMethodException {
        Method sendMethod = MessageChannel.class.getMethod("send", Message.class);
        when(invocation.getMethod()).thenReturn(sendMethod);
        when(invocation.getArguments()).thenReturn(new Object[] { message });
    }

    private void stubMethodInvocation(Method method, Object proceedResult) throws Throwable {
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.proceed()).thenReturn(proceedResult);
    }
}
