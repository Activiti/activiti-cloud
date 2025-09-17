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
package org.springframework.cloud.function.context.catalog;

import java.lang.reflect.Type;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Shadow class that overrides Spring Cloud Function's FunctionTypeUtils.
 * This ensures that any code trying to use the original Spring Cloud Function
 * FunctionTypeUtils will instead use our custom implementation.
 */
public final class FunctionTypeUtils {

    private FunctionTypeUtils() {
        // Utility class
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Class<?> getRawType(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getRawType(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type functionType(Type input, Type output) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.functionType(input, output);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type discoverFunctionType(Object bean, String beanName, GenericApplicationContext applicationContext) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionType(bean, beanName, applicationContext);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getInputType(Type functionType) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getInputType(functionType);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getOutputType(Type functionType) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getOutputType(functionType);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isFunction(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isFunction(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isConsumer(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isConsumer(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isSupplier(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isSupplier(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isTypeCollection(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isTypeCollection(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isTypeArray(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isTypeArray(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getGenericType(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getGenericType(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getImmediateGenericType(Type type, int index) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getImmediateGenericType(type, index);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isMessage(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isMessage(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isPublisher(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isPublisher(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isFlux(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isFlux(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isMono(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isMono(type);
    }

    // Add any other methods that might be called by Spring Cloud Function
    // All methods delegate to our custom implementation

    static {
        System.out.println("SUCCESS: Shadow FunctionTypeUtils loaded - all calls will be delegated to Activiti Cloud's custom implementation");
    }
}
