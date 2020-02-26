package org.activiti.cloud.services.audit.api.converters;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class APIEventToEntityConverters {

    private Map<String, EventToEntityConverter> converters;

    public APIEventToEntityConverters(Set<EventToEntityConverter> convertersSet) {
        this.converters = convertersSet.stream().collect(Collectors.toMap(EventToEntityConverter::getSupportedEvent,
                                                                          Function.identity()));
    }

    public Map<String, EventToEntityConverter> getConverters() {
        return converters;
    }

    public EventToEntityConverter getConverterByEventTypeName(String name) {
        return converters.get(name);
    }
}
