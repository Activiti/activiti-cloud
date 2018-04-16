package org.activiti.cloud.starter.rb.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("org.activiti.cloud.services")
@PropertySources({
        @PropertySource("classpath:metadata.properties"),
        @PropertySource("classpath:metadata-eureka.properties") //will have no effect when running without eureka
})
public class ActivitiRuntimeBundleAutoConfiguration {

}
