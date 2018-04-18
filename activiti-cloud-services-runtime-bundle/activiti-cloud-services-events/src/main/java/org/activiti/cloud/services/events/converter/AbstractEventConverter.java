package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.events.builders.ApplicationBuilderService;
import org.activiti.cloud.services.events.builders.ServiceBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractEventConverter implements EventConverter{

    private final ApplicationBuilderService applicationBuilderService;

    private final ServiceBuilderService serviceBuilderService;

    @Autowired
    public AbstractEventConverter(ApplicationBuilderService applicationBuilderService, ServiceBuilderService serviceBuilderService){
        this.applicationBuilderService = applicationBuilderService;
        this.serviceBuilderService = serviceBuilderService;
    }

    protected Application buildApplication(){
        return applicationBuilderService.buildApplication();
    }

    protected Service buildService(){
        return serviceBuilderService.buildService();
    }
}
