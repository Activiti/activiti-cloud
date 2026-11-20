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
package org.activiti.cloud.services.notifications.graphql.ws.liveness;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = GraphQlWebSocketLivenessProperties.PREFIX)
public class GraphQlWebSocketLivenessProperties {

    public static final String PREFIX = "spring.activiti.cloud.services.notification.graphql.ws.liveness";

    /**
     * Whether to close GraphQL WebSocket sessions whose client stopped sending messages, e.g. answering
     * keep-alive pings.
     */
    private boolean enabled = true;

    /**
     * Maximum time without any message from the client before the session is closed. Must be longer than
     * spring.graphql.websocket.keep-alive.
     */
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * How often sessions are checked against the timeout.
     */
    private Duration checkInterval = Duration.ofSeconds(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Duration checkInterval) {
        this.checkInterval = checkInterval;
    }
}
