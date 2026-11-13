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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

@ExtendWith(MockitoExtension.class)
class PollLockAdviceTest {

    private static final String LOCK_KEY = "test-poll-lock";

    private LockRegistry<Lock> lockRegistry;
    private PollLockAdvice advice;

    @Mock
    private MethodInvocation invocation;

    @BeforeEach
    void setUp() {
        lockRegistry = new DefaultLockRegistry();
        advice = new PollLockAdvice(lockRegistry, LOCK_KEY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invocationScenarios")
    void invoke_handlesLockingAndExecution(
        String scenarioName,
        Method method,
        boolean preLockOnAnotherThread,
        Object proceedResult,
        Object expectedResult,
        boolean expectProceed
    ) throws Throwable {
        // Arrange
        when(invocation.getMethod()).thenReturn(method);
        if (expectProceed) {
            when(invocation.proceed()).thenReturn(proceedResult);
        }

        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch releaseLockLatch = new CountDownLatch(1);

        try (ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor()) {
            if (preLockOnAnotherThread) {
                backgroundExecutor.submit(() -> {
                    Lock lock = lockRegistry.obtain(LOCK_KEY);
                    lock.lock();
                    try {
                        lockHeldLatch.countDown();
                        releaseLockLatch.await(1, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lock.unlock();
                    }
                });
                assertThat(lockHeldLatch.await(1, TimeUnit.SECONDS)).isTrue();
            }

            // Act
            Object actualResult = advice.invoke(invocation);

            // Assert
            assertThat(actualResult).isEqualTo(expectedResult);
            verify(invocation, expectProceed ? times(1) : never()).proceed();

            // Verify lock is free on the test thread after invocation completes
            if (!preLockOnAnotherThread) {
                Lock checkLock = lockRegistry.obtain(LOCK_KEY);
                assertThat(checkLock.tryLock()).isTrue();
                checkLock.unlock();
            }
        } finally {
            releaseLockLatch.countDown();
        }
    }

    private static Stream<Arguments> invocationScenarios() throws NoSuchMethodException {
        Method callMethod = Callable.class.getMethod("call");
        Method runMethod = Runnable.class.getMethod("run");

        return Stream.of(
            arguments(
                "call() with lock free -> proceeds and releases lock",
                callMethod,
                false,
                "poll-result",
                "poll-result",
                true
            ),
            arguments(
                "call() with lock held by another thread -> skips proceed and returns null",
                callMethod,
                true,
                "ignored",
                null,
                false
            ),
            arguments(
                "non-call() method -> bypasses lock even if held and proceeds",
                runMethod,
                true,
                "other-result",
                "other-result",
                true
            )
        );
    }

    @ParameterizedTest(name = "Propagates {0} and releases real lock")
    @ValueSource(classes = { IllegalStateException.class, RuntimeException.class })
    void invoke_whenProceedThrowsException_unlocksAndPropagates(Class<? extends Throwable> exceptionType)
        throws Throwable {
        // Arrange
        Method callMethod = Callable.class.getMethod("call");
        Throwable simulatedError = exceptionType.getConstructor(String.class).newInstance("Poll failure");

        when(invocation.getMethod()).thenReturn(callMethod);
        when(invocation.proceed()).thenThrow(simulatedError);

        // Act & Assert
        assertThatThrownBy(() -> advice.invoke(invocation))
            .isInstanceOf(exceptionType)
            .hasMessage("Poll failure");

        // Verify the real lock was properly released by the finally block
        Lock lock = lockRegistry.obtain(LOCK_KEY);
        assertThat(lock.tryLock()).isTrue();
        lock.unlock();
    }
}
