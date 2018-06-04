package org.activiti.cloud.services.metadata.eureka.dynamic;

import com.netflix.appinfo.ApplicationInfoManager;
import org.activiti.cloud.services.metadata.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnClass(ApplicationInfoManager.class)
@ConditionalOnProperty(name = "activiti.cloud.services.metadata.eureka.dynamic.enabled", matchIfMissing = true)
// This code needs to live here until we find the right abstraction for registering/updating Service Metadata
public class DynamicRegistrationConfiguration {

    @Autowired
    private ApplicationInfoManager appInfoManager;

    @Autowired
    private MetadataService metadataService;

    @PostConstruct
    public void init(){
        appInfoManager.registerAppMetadata(metadataService.getMetadata());
    }

}
