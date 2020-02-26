package org.activiti.cloud.starter.query.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(QuerySwaggerConfig.class)
public class ActivitiQueryAutoConfiguration {

}
