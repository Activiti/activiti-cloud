package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractEventConverter implements EventConverter{

    private final RuntimeBundleProperties runtimeBundleProperties;

    @Autowired
    public AbstractEventConverter(RuntimeBundleProperties runtimeBundleProperties) {
        this.runtimeBundleProperties = runtimeBundleProperties;
    }

    public String getFullyQualifiedServiceName() {
        return runtimeBundleProperties.getFullyQualifiedServiceName();
    }
}
