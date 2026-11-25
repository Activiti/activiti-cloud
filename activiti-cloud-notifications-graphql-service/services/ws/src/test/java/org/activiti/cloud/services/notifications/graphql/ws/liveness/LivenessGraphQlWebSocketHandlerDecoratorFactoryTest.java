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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

class LivenessGraphQlWebSocketHandlerDecoratorFactoryTest {

    private final GraphQlWebSocketLivenessMonitor monitor = mock(GraphQlWebSocketLivenessMonitor.class);
    private final LivenessGraphQlWebSocketHandlerDecoratorFactory factory =
        new LivenessGraphQlWebSocketHandlerDecoratorFactory(monitor);

    @Test
    void should_wrapDelegateWithLivenessHandler_when_decorating() {
        WebSocketHandler delegate = mock(WebSocketHandler.class);

        WebSocketHandlerDecorator decorated = factory.decorate(delegate);

        assertThat(decorated).isInstanceOf(LivenessGraphQlWebSocketHandler.class);
        assertThat(decorated.getDelegate()).isSameAs(delegate);
    }
}
