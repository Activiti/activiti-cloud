/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.jpa.config;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.jpa.ModelJpaRepository;
import org.activiti.cloud.services.modeling.jpa.ModelRepositoryImpl;
import org.activiti.cloud.services.modeling.jpa.audit.AuditorAwareImpl;
import org.activiti.cloud.services.modeling.jpa.version.ExtendedJpaRepositoryFactoryBean;
import org.activiti.cloud.services.modeling.jpa.version.VersionGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableJpaRepositories(
    basePackages = { "org.activiti.cloud.services.modeling.jpa" },
    repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class
)
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "localDateTimeProvider")
@EntityScan("org.activiti.cloud.services.modeling.entity")
public class ModelingJpaAutoConfiguration {

    @Bean("localDateTimeProvider")
    @ConditionalOnMissingBean(DateTimeProvider.class)
    public DateTimeProvider localDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
    }

    @Bean("auditorAware")
    public AuditorAware<String> auditorAware(SecurityManager securityManager) {
        return new AuditorAwareImpl(securityManager);
    }

    @Bean
    public VersionGenerator VersionGenerator() {
        return new VersionGenerator();
    }

    @Bean
    public ModelRepository modelRepository(ModelJpaRepository modelJpaRepository) {
        return new ModelRepositoryImpl(modelJpaRepository);
    }
}
