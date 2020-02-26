package org.activiti.cloud.services.metadata;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("activiti.cloud")
public class MetadataProperties implements InitializingBean {

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