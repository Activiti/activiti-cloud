package org.activiti.cloud.services.audit.api.converters;

import org.activiti.runtime.api.event.CloudRuntimeEvent;

public interface EventToEntityConverter<T> {

    String getSupportedEvent();

    T convertToEntity(CloudRuntimeEvent cloudRuntimeEvent);

    CloudRuntimeEvent convertToAPI(T eventEntity);
}
