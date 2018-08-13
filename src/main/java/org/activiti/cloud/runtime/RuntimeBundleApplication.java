package org.activiti.cloud.runtime;

import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ActivitiRuntimeBundle
public class RuntimeBundleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuntimeBundleApplication.class,
                              args);
    }
}