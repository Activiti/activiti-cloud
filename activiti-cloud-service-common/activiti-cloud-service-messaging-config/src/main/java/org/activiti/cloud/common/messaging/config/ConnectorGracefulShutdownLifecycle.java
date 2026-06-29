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
package org.activiti.cloud.common.messaging.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.context.SmartLifecycle;

public class ConnectorGracefulShutdownLifecycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorGracefulShutdownLifecycle.class);

    private static final long POLL_INTERVAL_MILLIS = 100;

    private final InFlightMessageTracker inFlightTracker;
    private final InputBindingLifecycle inputBindingLifecycle;
    private final Duration shutdownTimeout;

    private volatile boolean running;

    public ConnectorGracefulShutdownLifecycle(
        InFlightMessageTracker inFlightTracker,
        InputBindingLifecycle inputBindingLifecycle,
        Duration shutdownTimeout
    ) {
        this.inFlightTracker = inFlightTracker;
        this.inputBindingLifecycle = inputBindingLifecycle;
        this.shutdownTimeout = shutdownTimeout;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        stopConsumption();
        drainInFlightRequests();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void stopConsumption() {
        LOGGER.info("Graceful shutdown started: stopping connector message consumption");
        inputBindingLifecycle.stop();
    }

    private void drainInFlightRequests() {
        final long deadline = System.nanoTime() + shutdownTimeout.toNanos();
        int remaining = inFlightTracker.inFlight();
        while (remaining > 0 && System.nanoTime() < deadline) {
            LOGGER.debug("Graceful shutdown: waiting for {} in-flight connector request(s) to finish", remaining);
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            }
            remaining = inFlightTracker.inFlight();
        }

        if (remaining > 0) {
            LOGGER.warn(
                "Graceful shutdown timeout ({}) exceeded; {} in-flight connector request(s) did not finish",
                shutdownTimeout,
                remaining
            );
        } else {
            LOGGER.info("Graceful shutdown: all in-flight connector requests finished");
        }
    }
}
