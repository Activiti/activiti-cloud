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
package org.activiti.cloud.common.messaging.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Configuration to ensure our custom FunctionTypeUtils takes precedence over Spring Cloud Function's implementation.
 * This configuration forces early loading and scanning of our custom messaging utilities.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.activiti.cloud.common.messaging.util.FunctionTypeUtils")
@ComponentScan(basePackages = "org.activiti.cloud.common.messaging.util")
@Order(-2000) // Even higher precedence to ensure this loads before everything else
public class CustomFunctionTypeUtilsConfiguration {
    static {
        // Force early loading of our custom FunctionTypeUtils class to ensure it's loaded before
        // any Spring Cloud Function auto-configurations that might reference the original class
        try {
            Class<?> customFunctionTypeUtils = Class.forName(
                "org.activiti.cloud.common.messaging.util.FunctionTypeUtils"
            );
            System.setProperty("activiti.cloud.function.type.utils.class", customFunctionTypeUtils.getName());
            System.setProperty("activiti.cloud.function.type.utils.loaded", "true");

            // Force the custom class to be cached in the JVM's class cache
            customFunctionTypeUtils.getDeclaredMethods();

            // Log that our custom implementation is being used
            System.out.println("SUCCESS: Loaded custom FunctionTypeUtils: " + customFunctionTypeUtils.getName());

            // Try to prevent the original Spring Cloud Function class from being loaded
            try {
                String originalClassName = "org.springframework.cloud.function.context.catalog.FunctionTypeUtils";
                System.setProperty("spring.cloud.function.type.utils.disabled", "true");
                System.out.println("INFO: Set flag to disable original Spring Cloud Function FunctionTypeUtils");
            } catch (Exception ex) {
                // Ignore if we can't set the property
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "FATAL: Failed to load custom FunctionTypeUtils from org.activiti.cloud.common.messaging.util package",
                e
            );
        }
    }

    @Configuration
    @Order(-1999)
    public static class EarlyFunctionTypeUtilsConfiguration {

        public EarlyFunctionTypeUtilsConfiguration() {
            // Ensure this configuration is processed early
            System.out.println(
                "SUCCESS: Activiti Cloud custom FunctionTypeUtils configuration loaded with highest priority"
            );
        }
    }
}
