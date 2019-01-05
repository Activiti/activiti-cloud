/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.events.transformer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;

/**
 * Transform flat list of engine events maps into hierarchical structure
 * grouped by supplied common key attributes and event type ->
 * i.e. [{processInstanceId, serviceName, appName, processDefinitionId, eventType:[{attr1, attr2, ...}]}, ... ]
 *
 */
public class EngineEventsTransformer implements Transformer {

    private final String[] attributeKeys;
    private final String eventTypeKey;

    public EngineEventsTransformer(List<String> attributeList, String eventTypeKey) {
        this.attributeKeys = attributeList.toArray(new String[] {});
        this.eventTypeKey = eventTypeKey;
    }

    @Override
    public List<EngineEvent> transform(List<Map<String,Object>> events) {
        return events.stream()
                .filter(this::isValid)
                .collect(Collectors.groupingBy(this::processEngineEventAttributes, Collectors.groupingBy(this::eventType)))
                .entrySet()
                    .stream()
                    .map(entry -> Stream.of(entry.getKey(), entry.getValue())
                         .collect(EngineEvent::new, Map::putAll, Map::putAll)
                    )
                    .collect(Collectors.toList());
    }

    private boolean isValid(Map<String, Object> event) {
        return event.get(eventTypeKey) != null;
    }

    private String eventType(Map<String, Object> map) {
        return map.get(eventTypeKey).toString();
    }

    private Map<String, Object> processEngineEventAttributes(Map<String, Object> map) {
        return Stream.of(attributeKeys)
           .map(key -> new AbstractMap.SimpleEntry<String, Object>(key, map.getOrDefault(key, "")))
           .collect(Collectors.toMap(e -> e.getKey(), v -> Optional.ofNullable(v.getValue()).orElse("")));
    }
}
