package org.activiti.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("activiti.cloud")
@RefreshScope
@Component
public class SecurityProperties implements InitializingBean {

    private Map<String, String> group = new HashMap<String, String>();
    private Map<String, String> user = new HashMap<String,String>();

    public Map<String, String> getGroup() {
        return this.group;
    }

    public Map<String, String> getUser() {
        return this.user;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // do nothing
    }
}