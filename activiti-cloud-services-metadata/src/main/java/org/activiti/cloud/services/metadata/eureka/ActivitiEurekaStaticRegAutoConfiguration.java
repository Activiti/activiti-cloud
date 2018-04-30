package org.activiti.cloud.services.metadata.eureka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.netflix.appinfo.ApplicationInfoManager;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.metadata.eureka.static.enabled", matchIfMissing = true)
@ConditionalOnClass(ApplicationInfoManager.class)
@PropertySource("classpath:metadata-eureka.properties")
public class ActivitiEurekaStaticRegAutoConfiguration {
}
