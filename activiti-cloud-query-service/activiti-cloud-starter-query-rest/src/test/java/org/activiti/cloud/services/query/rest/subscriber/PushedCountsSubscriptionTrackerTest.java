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
package org.activiti.cloud.services.query.rest.subscriber;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PushedCountsSubscriptionTrackerTest {

    private final PushedCountsSubscriptionTracker tracker = new PushedCountsSubscriptionTracker();

    @Test
    void should_reportZeroToOneTransition_when_firstSubscriptionIsAdded() {
        assertThat(tracker.incrementAndCheckIfWasZero("session-1")).isTrue();
    }

    @Test
    void should_notReportTransition_when_secondSubscriptionIsAddedOnTheSameSession() {
        tracker.incrementAndCheckIfWasZero("session-1");

        assertThat(tracker.incrementAndCheckIfWasZero("session-1")).isFalse();
    }

    @Test
    void should_reportOneToZeroTransition_when_theLastSubscriptionIsRemoved() {
        tracker.incrementAndCheckIfWasZero("session-1");

        assertThat(tracker.decrementAndCheckIfNowZero("session-1")).isTrue();
    }

    @Test
    void should_notReportTransition_when_oneOfTwoSubscriptionsIsRemoved() {
        tracker.incrementAndCheckIfWasZero("session-1");
        tracker.incrementAndCheckIfWasZero("session-1");

        assertThat(tracker.decrementAndCheckIfNowZero("session-1")).isFalse();
    }

    @Test
    void should_reportTransitionAgain_when_aNewSubscriptionIsAddedAfterGoingBackToZero() {
        tracker.incrementAndCheckIfWasZero("session-1");
        tracker.decrementAndCheckIfNowZero("session-1");

        assertThat(tracker.incrementAndCheckIfWasZero("session-1")).isTrue();
    }

    @Test
    void should_trackSessionsIndependently_when_twoDifferentSessionsSubscribe() {
        assertThat(tracker.incrementAndCheckIfWasZero("session-1")).isTrue();
        assertThat(tracker.incrementAndCheckIfWasZero("session-2")).isTrue();
    }

    @Test
    void should_beANoOp_when_decrementingASessionThatWasNeverIncremented() {
        assertThat(tracker.decrementAndCheckIfNowZero("session-does-not-exist")).isFalse();
    }

    @Test
    void should_produceExactlyOneZeroToOneTransition_when_manyThreadsIncrementTheSameSessionConcurrently()
        throws InterruptedException {
        int threadCount = 32;
        AtomicInteger wasZeroCount = new AtomicInteger();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        if (tracker.incrementAndCheckIfWasZero("session-1")) {
                            wasZeroCount.incrementAndGet();
                        }
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
        }

        assertThat(wasZeroCount).hasValue(1);
    }
}
