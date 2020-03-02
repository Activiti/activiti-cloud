package org.activiti.cloud.starter.query.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(QuerySwaggerConfig.class)
@PropertySource("classpath:activiti-cloud-query-service.properties")
public class ActivitiQueryAutoConfiguration {

}
