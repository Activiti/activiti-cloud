package org.activiti.cloud.services.modeling.jpa.config;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.modeling.jpa.audit.AuditorAwareImpl;
import org.activiti.cloud.services.modeling.jpa.version.ExtendedJpaRepositoryFactoryBean;
import org.activiti.cloud.services.modeling.jpa.version.VersionGenerator;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.activiti.cloud.services.modeling.jpa"},
                       repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan("org.activiti.cloud.services.modeling.entity")
public class ModelingJpaAutoConfiguration {

    @Bean("auditorAware")
    public AuditorAware<String> auditorAware(SecurityManager securityManager) {
        return new AuditorAwareImpl(securityManager);
    }

    @Bean
    public VersionGenerator VersionGenerator() {
        return new VersionGenerator();
    }

}
