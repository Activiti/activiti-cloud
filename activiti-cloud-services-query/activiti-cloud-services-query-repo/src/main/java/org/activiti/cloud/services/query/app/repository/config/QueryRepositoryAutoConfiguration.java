package org.activiti.cloud.services.query.app.repository.config;

import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = ProcessInstanceRepository.class)
@EntityScan(basePackageClasses = ProcessInstanceEntity.class)
public class QueryRepositoryAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public EntityFinder entityFinder() {
        return new EntityFinder(); 
    }

}
