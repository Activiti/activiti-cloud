package org.activiti.cloud.runtime;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.activiti.cloud.services.metadata.MetadataService;

@Configuration
@ConditionalOnClass(ApplicationInfoManager.class)
public class RuntimeBundleMetaDataConfiguration {

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ApplicationInfoManager appInfoManager;
    // This code needs to live here until we find the right abstraction for registering/updating Service Metadata

    public RuntimeBundleMetaDataConfiguration() {

    }

    @PostConstruct
    public void init() {
        appInfoManager.registerAppMetadata(metadataService.getMetadata());
    }
}