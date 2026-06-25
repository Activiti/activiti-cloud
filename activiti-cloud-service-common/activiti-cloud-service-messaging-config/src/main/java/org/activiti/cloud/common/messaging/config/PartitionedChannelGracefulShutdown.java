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
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.support.ChannelInterceptor;

public class PartitionedChannelGracefulShutdown implements SmartLifecycle {

    private final InFlightMessageTracker inFlightTracker;
    private final ConnectorGracefulShutdownLifecycle lifecycle;

    public PartitionedChannelGracefulShutdown(InputBindingLifecycle inputBindingLifecycle, Duration shutdownTimeout) {
        this.inFlightTracker = new InFlightMessageTracker();
        this.lifecycle = new ConnectorGracefulShutdownLifecycle(
            inFlightTracker,
            inputBindingLifecycle,
            shutdownTimeout
        );
    }

    public ChannelInterceptor channelInterceptor() {
        return inFlightTracker;
    }

    public int inFlight() {
        return inFlightTracker.inFlight();
    }

    @Override
    public void start() {
        lifecycle.start();
    }

    @Override
    public void stop() {
        lifecycle.stop();
    }

    @Override
    public boolean isRunning() {
        return lifecycle.isRunning();
    }

    @Override
    public int getPhase() {
        return lifecycle.getPhase();
    }
}
