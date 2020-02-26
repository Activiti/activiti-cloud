package org.activiti.cloud.common.properties;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:conf/common-configuration.properties")
public class CommonPropertiesAutoConfiguration {
}
