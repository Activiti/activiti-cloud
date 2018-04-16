package org.activiti.cloud.services.metadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.metadata.enabled", matchIfMissing = true)
public class ActivitiMetadataAutoConfiguration {

    @Autowired
    private MetadataProperties metadataProperties;

    @Bean
    @ConditionalOnMissingBean(type="MetadataService")
    MetadataService metadataService(){
        return new MetadataService(metadataProperties);
    }
}
