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

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.CONNECTOR_TYPE;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.Message;

public class FunctionRouterMessageDestinationResolver implements Function<Message<?>, String> {

    private final ActivitiCloudMessagingProperties messagingProperties;

    public FunctionRouterMessageDestinationResolver(ActivitiCloudMessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Override
    public String apply(Message<?> message) {
        return Optional.of(message.getHeaders())
            .flatMap(headers ->
                Optional.ofNullable(headers.get(FunctionRouterMessageHeaders.FUNCTION_DESTINATION, String.class))
                    .or(() -> Optional.ofNullable(message.getHeaders().get(CONNECTOR_TYPE, String.class)))
                    .or(() ->
                        Optional.ofNullable(messagingProperties.getRabbitmq().getPrefix())
                            .filter(Predicate.not(String::isBlank))
                            .flatMap(prefix ->
                                Optional.ofNullable(
                                    message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class)
                                )
                                    .filter(exchange -> exchange.startsWith(prefix))
                                    .map(exchange -> exchange.substring(prefix.length()))
                            )
                            .or(() -> Optional.ofNullable(headers.get(AmqpHeaders.RECEIVED_EXCHANGE, String.class)))
                    )
            )
            .filter(it ->
                Optional.ofNullable(messagingProperties.getFunctionRouter().registrations().get(it))
                    .filter(Predicate.not(Collection::isEmpty))
                    .isPresent()
            )
            .orElseThrow(() -> new MessageDispatchingException(message, "Missing route destination"));
    }
}
