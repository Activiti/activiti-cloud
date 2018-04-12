package org.activiti.cloud.runtime;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

import com.netflix.appinfo.ApplicationInfoManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ApplicationInfoManager.class)
public class RuntimeBundleMetaDataConfiguration {

    @Value("${activiti.cloud.application.name:}")
    private String applicationName;

    @Autowired
    private ApplicationInfoManager appInfoManager;
    // This code needs to live here until we find the right abstraction for registering/updating Service Metadata

    public RuntimeBundleMetaDataConfiguration() {

    }

    @PostConstruct
    public void init() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("activiti-cloud-service-type",
                     "runtime-bundle");
        metadata.put("activiti-cloud-application-name",
                     applicationName);
        appInfoManager.registerAppMetadata(metadata);
    }
}