package org.activit.cloud.services.metadata;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("activiti.cloud")
@RefreshScope
@Component
public class ActivitiCloudMetadataProperties implements InitializingBean {

    private Map<String, String> application = new HashMap<String, String>();
    private Map<String, String> service = new HashMap<String,String>();

    public Map<String, String> getApplication() {
        return this.application;
    }

    public Map<String, String> getService() {
        return this.service;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // do nothing
    }
}