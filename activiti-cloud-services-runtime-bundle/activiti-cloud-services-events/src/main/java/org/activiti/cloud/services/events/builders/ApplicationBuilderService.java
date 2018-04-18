package org.activiti.cloud.services.events.builders;

import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationBuilderService {

    @Autowired
    private RuntimeBundleProperties runtimeBundleProperties;

    public Application buildApplication(){
        Application application = new Application(runtimeBundleProperties.getActivitiAppName(),runtimeBundleProperties.getActivitiAppVersion());
        return application;
    }
}
