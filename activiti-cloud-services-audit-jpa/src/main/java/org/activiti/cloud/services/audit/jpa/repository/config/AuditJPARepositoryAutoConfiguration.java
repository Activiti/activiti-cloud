package org.activiti.cloud.services.audit.jpa.repository.config;

import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = EventsRepository.class)
@EntityScan(basePackageClasses = AuditEventEntity.class)
public class AuditJPARepositoryAutoConfiguration {

}
