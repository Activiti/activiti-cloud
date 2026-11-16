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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.activiti.cloud.common.messaging.config.StreamBridgeOperations;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

class OutboxStreamBridgeAdviceTest {

    private final OutboxStreamBridgeAdvice advice = new OutboxStreamBridgeAdvice();

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideInvocationScenarios")
    void shouldInterceptMethodInvocation(
        String scenario,
        String methodName,
        Object[] arguments,
        Class<?>[] parameterTypes,
        Object proceedResult,
        Object expectedResult
    ) throws Throwable {
        // Given
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = StreamBridgeOperations.class.getMethod(methodName, parameterTypes);

        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(arguments);
        when(invocation.proceed()).thenReturn(proceedResult);

        // When
        Object actualResult = advice.invoke(invocation);

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> provideInvocationScenarios() {
        Message<String> messageWithOutboxHeader = MessageBuilder.withPayload("payload")
            .setHeader(OUTBOX_DESTINATION, "existing-destination")
            .build();

        Message<String> messageWithoutOutboxHeader = MessageBuilder.withPayload("payload").build();

        return Stream.of(
            // 2. "send" method returns true -> short-circuits to true regardless of headers
            Arguments.of(
                "Send returns true when proceed() is true even if OUTBOX_DESTINATION header exists",
                "send",
                new Object[] { "bindingName", messageWithOutboxHeader },
                new Class<?>[] { String.class, Object.class },
                true,
                true
            ),

            // 3. <= 3 args, Message WITH header, proceed() is false -> false
            Arguments.of(
                "Send (2 args) returns false when proceed() is false and OUTBOX_DESTINATION header exists",
                "send",
                new Object[] { "bindingName", messageWithOutboxHeader },
                new Class<?>[] { String.class, Object.class },
                false,
                false
            ),

            // 4. <= 3 args, Message WITHOUT header, proceed() is false -> true
            Arguments.of(
                "Send (2 args) returns true when proceed() is false and OUTBOX_DESTINATION header is missing",
                "send",
                new Object[] { "bindingName", messageWithoutOutboxHeader },
                new Class<?>[] { String.class, Object.class },
                false,
                true
            ),

            // 5. <= 3 args, Raw Object payload (wrapped automatically, has no header), proceed() is false -> true
            Arguments.of(
                "Send (3 args) with raw payload returns true when proceed() is false",
                "send",
                new Object[] { "bindingName", "rawPayload", "contentType" },
                new Class<?>[] { String.class, Object.class, MimeType.class },
                false,
                true
            ),

            // 6. > 3 args (4 args, checks index 2), Message WITH header, proceed() is false -> false
            Arguments.of(
                "Send (4 args) checks index 2 and returns false when OUTBOX_DESTINATION header exists",
                "send",
                new Object[] { "bindingName", "binderName", messageWithOutboxHeader, "contentType" },
                new Class<?>[] { String.class, String.class, Object.class, MimeType.class },
                false,
                false
            ),

            // 7. > 3 args (4 args, checks index 2), Message WITHOUT header, proceed() is false -> true
            Arguments.of(
                "Send (4 args) checks index 2 and returns true when OUTBOX_DESTINATION header is missing",
                "send",
                new Object[] { "bindingName", "binderName", messageWithoutOutboxHeader, "contentType" },
                new Class<?>[] { String.class, String.class, Object.class, MimeType.class },
                false,
                true
            ),

            // 8. > 3 args (4 args, checks index 2), Raw Object payload, proceed() is false -> true
            Arguments.of(
                "Send (4 args) with raw payload at index 2 returns true when proceed() is false",
                "send",
                new Object[] { "bindingName", "binderName", "rawPayload", "contentType" },
                new Class<?>[] { String.class, String.class, Object.class, MimeType.class },
                false,
                true
            )
        );
    }

    private Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        return types;
    }
}
