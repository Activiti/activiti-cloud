package org.activiti.cloud.connectors.starter.configuration;

import org.conf.activiti.runtime.CloudProcessModelAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan(basePackages = {"org.activiti.cloud.connectors.starter", "org.activiti.cloud.services.common.security"})
@Import(CloudProcessModelAutoConfiguration.class)
public class ActivitiCloudConnectorAutoConfiguration {
}
