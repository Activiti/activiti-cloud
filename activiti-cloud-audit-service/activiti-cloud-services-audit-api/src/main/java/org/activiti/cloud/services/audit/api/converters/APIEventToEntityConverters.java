/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.audit.api.converters;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class APIEventToEntityConverters {

    private Map<String, EventToEntityConverter> converters;

    public APIEventToEntityConverters(Set<EventToEntityConverter> convertersSet) {
        this.converters =
            convertersSet
                .stream()
                .collect(Collectors.toMap(EventToEntityConverter::getSupportedEvent, Function.identity()));
    }

    public Map<String, EventToEntityConverter> getConverters() {
        return converters;
    }

    public EventToEntityConverter getConverterByEventTypeName(String name) {
        return converters.get(name);
    }
}
