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
package org.activiti.cloud.common.messaging.function.router;

import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTE;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTE_CORRELATION_ID;

import org.jspecify.annotations.Nullable;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.messaging.Message;

public class FunctionRouterIdempotentInterceptorKeyStrategy implements MessageProcessor<String> {

    @Override
    public @Nullable String processMessage(Message<?> message) {
        return (
            message.getHeaders().getOrDefault(ROUTE, "unknown") + ":" + message.getHeaders().get(ROUTE_CORRELATION_ID)
        );
    }
}
