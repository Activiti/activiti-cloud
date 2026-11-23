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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

public class CircuitBreakerPollerAdvice implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerPollerAdvice.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN,
    }

    private final int failureThreshold;
    private final long openDurationMillis;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAtMillis = new AtomicLong(0);

    public CircuitBreakerPollerAdvice(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDurationMillis = openDuration.toMillis();
    }

    @Nullable
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();

        log.debug("Invoking poller circuit breaker advice on target: {}", target);

        Message<?> result = null;
        if (beforeReceive()) {
            try {
                result = (Message<?>) invocation.proceed();
                closeAndResetCircuit();
            } catch (Throwable error) {
                openCircuitIfThresholdReached();

                throw error;
            }
        }
        return afterReceive(result);
    }

    protected boolean beforeReceive() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true; // Proceed with receive
        }
        if (current == State.OPEN) {
            if (System.currentTimeMillis() - openedAtMillis.get() >= openDurationMillis) {
                // Transition to HALF_OPEN and allow a single probe message to be received
                return state.compareAndSet(State.OPEN, State.HALF_OPEN);
            }
            return false; // Circuit is OPEN: skip receive() completely
        }
        // Already HALF_OPEN: block concurrent polls until the in-flight probe completes
        return false;
    }

    @SuppressWarnings("java:S1452")
    protected Message<?> afterReceive(Message<?> result) {
        // If the outbox table was empty during a HALF_OPEN probe, reset back to HALF_OPEN or CLOSED
        if (result == null && state.get() == State.HALF_OPEN) {
            state.set(State.CLOSED);
            consecutiveFailures.set(0);
        }
        return result;
    }

    private void closeAndResetCircuit() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    private void openCircuitIfThresholdReached() {
        if (state.get() == State.HALF_OPEN || consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openedAtMillis.set(System.currentTimeMillis());
            state.set(State.OPEN);
        }
    }
}
