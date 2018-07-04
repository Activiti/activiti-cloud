package org.activiti.cloud.services.audit.api.converters;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class APIEventToEntityConverters {

    private static Logger LOGGER = LoggerFactory.getLogger(APIEventToEntityConverters.class);

    private Map<String, EventToEntityConverter> converters;

    @Autowired
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
