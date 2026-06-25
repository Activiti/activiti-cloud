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
package org.activiti.cloud.services.notifications.graphql.events;

import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;

public class EngineEventRoutingKeyResolver implements RoutingKeyResolver {

    private static final String DEFAULT_VALUE = "_";

    @Override
    public String resolveRoutingKey(Object object) {
        if (object instanceof EngineEvent engineEvent) {
            return "engineEvents.%s.%s.%s.%s.%s.%s.%s".formatted(
                segment(engineEvent, "serviceName"),
                segment(engineEvent, "appName"),
                segment(engineEvent, "eventType"),
                segment(engineEvent, "processDefinitionKey"),
                segment(engineEvent, "processInstanceId"),
                segment(engineEvent, "businessKey"),
                segment(engineEvent, "actor")
            );
        }

        throw new IllegalArgumentException(
            "Cannot resolve routing key for class: " + (object == null ? "null" : object.getClass())
        );
    }

    private static String segment(EngineEvent engineEvent, String key) {
        Object value = engineEvent.get(key);
        if (value == null) {
            return DEFAULT_VALUE;
        }
        String asString = value.toString();
        return asString.isBlank() ? DEFAULT_VALUE : asString;
    }
}
