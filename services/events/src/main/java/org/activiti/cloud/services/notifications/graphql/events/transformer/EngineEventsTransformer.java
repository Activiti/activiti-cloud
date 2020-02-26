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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;

/**
 * Transform flat list of engine events maps into linear structure with validation of eventType key
 */
public class EngineEventsTransformer implements Transformer {

    private final List<String> attributeList;
    private final String eventTypeKey;

    public EngineEventsTransformer(List<String> attributeList, String eventTypeKey) {
        this.attributeList = attributeList;
        this.eventTypeKey = eventTypeKey;
    }

    @Override
    public List<EngineEvent> transform(List<Map<String,Object>> events) {
        return events.stream()
                .filter(this::isValid)
                .map(it -> new EngineEvent(it))
                .collect(Collectors.toList());
    }

    private boolean isValid(Map<String, Object> event) {
        return event.get(eventTypeKey) != null;
    }
}
