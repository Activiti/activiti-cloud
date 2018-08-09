package org.activiti.cloud.starter.query.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableJpaRepositories({"org.activiti.cloud.services.query.app.repository"})
@EntityScan(basePackages = {"org.activiti.cloud.services.query.model"})
@Inherited
@EnableDiscoveryClient
@EnableAutoConfiguration
@EnableWebSecurity
public @interface EnableActivitiQuery {

}
