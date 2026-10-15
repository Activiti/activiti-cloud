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

import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.FUNCTION_DEFINITION;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.FUNCTION_DESTINATION;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.ErrorMessage;

@MessagingGateway(defaultRequestChannel = FunctionRouterGateway.FUNCTION_ROUTER_GATEWAY_INPUT_CHANNEL)
public interface FunctionRouterGateway extends Function<Message<?>, FunctionRouterGateway.RouteResult> {
    String FUNCTION_ROUTER_GATEWAY_INPUT_CHANNEL = "functionRouterGatewayInputChannel";

    RouteResult apply(Message<?> message);

    CompletableFuture<RouteResult> applyAsync(Message<?> message);

    RouteResult forwardTo(@Header(FUNCTION_DESTINATION) String destination, Message<?> message);

    CompletableFuture<RouteResult> forwardToAsync(@Header(FUNCTION_DESTINATION) String destination, Message<?> message);

    RouteResult routeTo(@Header(FUNCTION_DEFINITION) String route, Message<?> message);

    CompletableFuture<RouteResult> routeToAsync(@Header(FUNCTION_DEFINITION) String route, Message<?> message);

    record RouteResult(Message<?> request, MessageGroup results) {
        public boolean hasErrors() {
            return results.getMessages().stream().anyMatch(ErrorMessage.class::isInstance);
        }

        public List<ErrorMessage> getErrors() {
            return results
                .getMessages()
                .stream()
                .filter(ErrorMessage.class::isInstance)
                .map(ErrorMessage.class::cast)
                .toList();
        }

        public List<? extends Exception> getCauses() {
            return getErrors()
                .stream()
                .map(ErrorMessage::getPayload)
                .filter(Predicate.not(MessageRejectedException.class::isInstance))
                .map(Exception.class::cast)
                .toList();
        }
    }
}
