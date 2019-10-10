package org.activiti.cloud.services.organization.jpa.config;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.organization.jpa.audit.AuditorAwareImpl;
import org.activiti.cloud.services.organization.jpa.version.ExtendedJpaRepositoryFactoryBean;
import org.activiti.cloud.services.organization.jpa.version.VersionGenerator;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.activiti.cloud.services.organization.jpa"},
                       repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan("org.activiti.cloud.services.organization.entity")
public class OrganizationJpaAutoConfiguration {

    @Bean("auditorAware")
    public AuditorAware<String> auditorAware(SecurityManager securityManager) {
        return new AuditorAwareImpl(securityManager);
    }
    
    @Bean
    public VersionGenerator VersionGenerator() {
        return new VersionGenerator();
    }
    
}
