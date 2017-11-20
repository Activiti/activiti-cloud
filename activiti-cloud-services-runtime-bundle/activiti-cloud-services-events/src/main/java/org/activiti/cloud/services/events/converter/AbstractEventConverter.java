package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.events.configuration.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractEventConverter implements EventConverter{

    private final ApplicationProperties applicationProperties;

    @Autowired
    public AbstractEventConverter(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getApplicationName() {
        return applicationProperties.getName();
    }
}
