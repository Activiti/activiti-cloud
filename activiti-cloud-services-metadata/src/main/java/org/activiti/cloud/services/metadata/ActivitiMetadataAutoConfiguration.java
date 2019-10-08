package org.activiti.cloud.services.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.metadata.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MetadataProperties.class)
@PropertySource(value = "classpath:metadata.properties",ignoreResourceNotFound = true)
public class ActivitiMetadataAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MetadataService.class)
    public MetadataService metadataService(MetadataProperties metadataProperties){
        return new MetadataService(metadataProperties);
    }
}
