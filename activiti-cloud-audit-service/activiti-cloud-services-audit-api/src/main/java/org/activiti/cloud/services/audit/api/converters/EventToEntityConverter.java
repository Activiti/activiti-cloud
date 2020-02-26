package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

public interface EventToEntityConverter<T> {

    String getSupportedEvent();

    T convertToEntity(CloudRuntimeEvent cloudRuntimeEvent);

    CloudRuntimeEvent convertToAPI(T eventEntity);
}
