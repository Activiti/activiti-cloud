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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class FunctionRouterPartitionKeySelector implements Function<Message<?>, Object> {

    private final Map<String, Integer> partitionsMap;
    private final Function<MessageHeaders, String> functionRouterMessageDestinationSelector;

    public FunctionRouterPartitionKeySelector(
        Supplier<List<String>> routesSupplier,
        Function<MessageHeaders, String> functionRouterMessageDestinationSelector
    ) {
        this.functionRouterMessageDestinationSelector = functionRouterMessageDestinationSelector;

        final var routes = routesSupplier.get();

        this.partitionsMap = IntStream.range(0, routes.size())
            .mapToObj(i -> Map.entry(routes.get(i), i + 1))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Object apply(Message<?> message) {
        final var route = functionRouterMessageDestinationSelector.apply(message.getHeaders());

        return partitionsMap.getOrDefault(route, 0);
    }
}
