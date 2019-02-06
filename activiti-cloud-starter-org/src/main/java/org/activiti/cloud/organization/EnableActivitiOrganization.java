/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.organization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.activiti.cloud.services.organization.jpa.version.ExtendedJpaRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration
@ComponentScan({"org.activiti.cloud.organization", "org.activiti.cloud.services.organization"})
@EnableJpaRepositories(
        basePackages = {"org.activiti.cloud.services.organization.jpa"},
        repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan("org.activiti.cloud.services.organization.entity")
@Inherited
@EnableDiscoveryClient
public @interface EnableActivitiOrganization {

}