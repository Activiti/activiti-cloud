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
import java.util.function.Function;
import java.util.stream.Stream;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;

public class FunctionRouterDestinationsProvider implements Function<String, List<String>> {

    private final ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties;

    public FunctionRouterDestinationsProvider(
        ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties
    ) {
        this.functionRouterProperties = functionRouterProperties;
    }

    @Override
    public List<String> apply(String routingContext) {
        return functionRouterProperties
            .destinations(routingContext)
            .values()
            .stream()
            .flatMap(it -> Stream.of(it.split(",")))
            .distinct()
            .toList();
    }
}
