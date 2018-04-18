package org.activiti.cloud.services.events.builders;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceBuilderService {

    @Autowired
    private RuntimeBundleProperties runtimeBundleProperties;

    public org.activiti.cloud.services.api.model.Service buildService(){
        org.activiti.cloud.services.api.model.Service service = new org.activiti.cloud.services.api.model.Service(
                runtimeBundleProperties.getFullyQualifiedServiceName(),
                runtimeBundleProperties.getServiceName(),
                runtimeBundleProperties.getServiceType(),
                runtimeBundleProperties.getServiceVersion()
        );
        return service;
    }
}
