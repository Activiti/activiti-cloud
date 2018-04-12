package org.activiti.cloud.starter.rb.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableBinding(ProcessEngineChannels.class)
@Inherited
@EnableDiscoveryClient
public @interface ActivitiRuntimeBundle {

}