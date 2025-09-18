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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to create a bean that shadows the Spring Cloud Function FunctionTypeUtils usage.
 * This ensures our custom implementation is used throughout the Spring context.
 */
@Configuration
@ConditionalOnProperty(
    value = "activiti.cloud.function.type.utils.override",
    havingValue = "true",
    matchIfMissing = true
)
public class FunctionTypeUtilsOverrideConfiguration {

    @Bean
    @Primary
    public FunctionTypeUtilsWrapper functionTypeUtilsWrapper() {
        return new FunctionTypeUtilsWrapper();
    }

    /**
     * Wrapper class that delegates to our custom FunctionTypeUtils implementation.
     * This ensures that any Spring context bean lookups use our implementation.
     */
    public static class FunctionTypeUtilsWrapper {

        public java.lang.reflect.Type functionType(java.lang.reflect.Type input, java.lang.reflect.Type output) {
            return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.functionType(input, output);
        }

        public Class<?> getRawType(java.lang.reflect.Type type) {
            return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getRawType(type);
        }

        public java.lang.reflect.Type discoverFunctionType(
            Object bean,
            String beanName,
            org.springframework.context.support.GenericApplicationContext applicationContext
        ) {
            return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionType(
                bean,
                beanName,
                applicationContext
            );
        }
    }
}
