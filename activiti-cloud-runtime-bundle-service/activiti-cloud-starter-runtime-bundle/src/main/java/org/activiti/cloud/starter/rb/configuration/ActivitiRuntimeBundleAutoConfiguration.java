package org.activiti.cloud.starter.rb.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SwaggerConfig.class)
public class ActivitiRuntimeBundleAutoConfiguration {

}
