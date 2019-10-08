package org.activiti.cloud.starter.rb.configuration;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableBinding(ProcessEngineChannels.class)
@Inherited
@EnableDiscoveryClient
@EnableWebSecurity
@EnableAutoConfiguration(exclude = {TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class})
public @interface ActivitiRuntimeBundle {

}