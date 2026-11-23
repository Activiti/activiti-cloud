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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInvocation;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

class CircuitBreakerPollerAdviceTest {

    @Test
    void closedState_allowsExecutionAndReturnsMessage() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(3, Duration.ofMillis(200));
        var expectedMessage = new GenericMessage<>("payload");
        var invocation = StubMethodInvocation.returning(expectedMessage);

        Object result = advice.invoke(invocation);

        assertThat(result).isSameAs(expectedMessage);
        assertThat(invocation.callCount()).isEqualTo(1);
    }

    @Test
    void failuresBelowThreshold_keepCircuitClosed() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(3, Duration.ofMillis(200));
        var boom = new RuntimeException("db down");

        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        // 3rd call succeeds -> circuit was still CLOSED
        var msg = new GenericMessage<>("ok");
        Object result = advice.invoke(StubMethodInvocation.returning(msg));

        assertThat(result).isSameAs(msg);
    }

    @Test
    void reachingFailureThreshold_opensCircuitAndShortCircuitsSubsequentCalls() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(2, Duration.ofSeconds(10));
        var boom = new RuntimeException("db down");

        // 2 failures hit the threshold -> transitions to OPEN
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        // Subsequent call while OPEN should skip proceed() and return null
        var blockedInvocation = StubMethodInvocation.returning(new GenericMessage<>("ignored"));
        Object result = advice.invoke(blockedInvocation);

        assertThat(result).isNull();
        assertThat(blockedInvocation.callCount()).isZero();
    }

    @Test
    void successBeforeThreshold_resetsConsecutiveFailureCount() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(2, Duration.ofSeconds(10));
        var boom = new RuntimeException("transient error");

        // 1 failure (below threshold of 2)
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        // 1 success resets failure count back to 0
        advice.invoke(StubMethodInvocation.returning(new GenericMessage<>("ok")));

        // Another single failure should NOT open the circuit
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        var nextInvocation = StubMethodInvocation.returning(new GenericMessage<>("still closed"));
        assertThat(advice.invoke(nextInvocation)).isNotNull();
        assertThat(nextInvocation.callCount()).isEqualTo(1);
    }

    @Test
    void afterOpenDuration_transitionsToHalfOpenAndClosesOnProbeSuccess() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(1, Duration.ofMillis(50));
        var boom = new RuntimeException("db down");

        // Trip circuit to OPEN
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        sleep(Duration.ofMillis(70));

        // First call after openDuration acts as HALF_OPEN probe and closes the circuit
        var probeMsg = new GenericMessage<>("recovered");
        assertThat(advice.invoke(StubMethodInvocation.returning(probeMsg))).isSameAs(probeMsg);

        // Next call proceeds normally in CLOSED state
        var normalMsg = new GenericMessage<>("normal");
        assertThat(advice.invoke(StubMethodInvocation.returning(normalMsg))).isSameAs(normalMsg);
    }

    @Test
    void halfOpenProbeReturningNull_closesCircuit() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(1, Duration.ofMillis(50));
        var boom = new RuntimeException("db down");

        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        sleep(Duration.ofMillis(70));

        // Empty poll (null result) during HALF_OPEN probe resets circuit to CLOSED
        var emptyProbe = StubMethodInvocation.returning(null);
        assertThat(advice.invoke(emptyProbe)).isNull();
        assertThat(emptyProbe.callCount()).isEqualTo(1);

        // Verify circuit is now CLOSED by sending a regular message
        var nextMsg = new GenericMessage<>("after-empty-probe");
        assertThat(advice.invoke(StubMethodInvocation.returning(nextMsg))).isSameAs(nextMsg);
    }

    @Test
    void halfOpenProbeFailure_reopensCircuitImmediately() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(3, Duration.ofMillis(50));
        var boom = new RuntimeException("db down");

        // Trip circuit with 3 failures
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);
        }

        sleep(Duration.ofMillis(70));

        // Single failure during HALF_OPEN immediately trips back to OPEN
        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        var blockedInvocation = StubMethodInvocation.returning(new GenericMessage<>("blocked"));
        assertThat(advice.invoke(blockedInvocation)).isNull();
        assertThat(blockedInvocation.callCount()).isZero();
    }

    @Test
    void concurrentPollWhileHalfOpen_isBlockedUntilProbeFinishes() throws Throwable {
        var advice = new CircuitBreakerPollerAdvice(1, Duration.ofMillis(50));
        var boom = new RuntimeException("db down");
        var inFlightProbeMessage = new GenericMessage<>("probe-result");

        assertThatThrownBy(() -> advice.invoke(StubMethodInvocation.throwing(boom))).isSameAs(boom);

        sleep(Duration.ofMillis(70));

        // Simulate an in-flight probe inside proceed() attempting a concurrent call
        var concurrentCallBlocked = new AtomicBoolean(false);
        var concurrentInvocation = StubMethodInvocation.returning(new GenericMessage<>("concurrent"));

        var inFlightProbe = new StubMethodInvocation(() -> {
            Object concurrentResult = advice.invoke(concurrentInvocation);
            concurrentCallBlocked.set(concurrentResult == null && concurrentInvocation.callCount() == 0);
            return inFlightProbeMessage;
        });

        Object probeResult = advice.invoke(inFlightProbe);

        assertThat(concurrentCallBlocked).isTrue();
        assertThat(probeResult).isSameAs(inFlightProbeMessage);
    }

    /**
     * Lightweight test double for AOP MethodInvocation without mocking libraries.
     */
    private static final class StubMethodInvocation implements MethodInvocation {

        @FunctionalInterface
        interface Action {
            @Nullable
            Object run() throws Throwable;
        }

        private final Action action;
        private final AtomicInteger calls = new AtomicInteger(0);

        private StubMethodInvocation(Action action) {
            this.action = action;
        }

        static StubMethodInvocation returning(@Nullable Message<?> message) {
            return new StubMethodInvocation(() -> message);
        }

        static StubMethodInvocation throwing(Throwable throwable) {
            return new StubMethodInvocation(() -> {
                throw throwable;
            });
        }

        int callCount() {
            return calls.get();
        }

        @Override
        public @Nullable Object proceed() throws Throwable {
            calls.incrementAndGet();
            return action.run();
        }

        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Override
        public Method getMethod() {
            throw new UnsupportedOperationException("Not needed for test");
        }

        @Override
        public @Nullable Object getThis() {
            return new Object();
        }

        @Override
        public AccessibleObject getStaticPart() {
            throw new UnsupportedOperationException("Not needed for test");
        }
    }

    private void sleep(Duration duration) {
        Awaitility.await()
            .pollInSameThread()
            .pollDelay(duration)
            .until(() -> true);
    }
}
