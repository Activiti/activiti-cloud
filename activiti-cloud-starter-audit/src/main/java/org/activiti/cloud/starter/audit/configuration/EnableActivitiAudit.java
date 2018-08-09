package org.activiti.cloud.starter.audit.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableJpaRepositories("org.activiti.cloud.services.audit.jpa")
@EntityScan(basePackages = {"org.activiti.cloud.services.audit.jpa.events","org.activiti.cloud.services.audit.jpa.events.model"})
@Inherited
@EnableDiscoveryClient
@EnableAutoConfiguration
@EnableWebSecurity
public @interface EnableActivitiAudit {

}