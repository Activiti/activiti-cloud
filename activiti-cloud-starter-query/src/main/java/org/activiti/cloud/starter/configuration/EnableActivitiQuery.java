package org.activiti.cloud.starter.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableJpaRepositories({"org.activiti.services.query.app.repository"})
@EntityScan(basePackages = {"org.activiti.services.query.model"})
@Inherited
@EnableDiscoveryClient
public @interface EnableActivitiQuery {

}
