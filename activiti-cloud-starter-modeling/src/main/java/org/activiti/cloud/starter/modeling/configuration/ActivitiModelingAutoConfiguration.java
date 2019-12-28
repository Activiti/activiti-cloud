package org.activiti.cloud.starter.modeling.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ModelingSwaggerConfig.class)
public class ActivitiModelingAutoConfiguration {

}
