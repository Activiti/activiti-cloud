package org.activiti.cloud.query;

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
public class QueryMetaDataAutoConfiguration {

    @Value("${activiti.cloud.application.name:}")
    private String applicationName;

    @Autowired
    private ApplicationInfoManager appInfoManager;

    // This code needs to live here until we find the right abstraction for registering/updating Service Metadata

    public QueryMetaDataAutoConfiguration() {

    }

    @PostConstruct
    public void init() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("activiti-cloud-service-type",
                     "query");
        metadata.put("activiti-cloud-application-name",
                     applicationName);
        appInfoManager.registerAppMetadata(metadata);
    }
}