package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractEventConverter implements EventConverter{

    private final RuntimeBundleProperties runtimeBundleProperties;

    @Autowired
    public AbstractEventConverter(RuntimeBundleProperties applicationProperties) {
        this.runtimeBundleProperties = applicationProperties;
    }

    public String getApplicationName() {
        return runtimeBundleProperties.getName();
    }
}
