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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
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
    public static Type consumerType(Type input) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.consumerType(input);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type supplierType(Type output) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.supplierType(output);
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
     * This is the missing method that was causing the NoSuchMethodError.
     */
    public static boolean isCollectionOfMessage(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isCollectionOfMessage(type);
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
    public static boolean isTypeMap(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isTypeMap(type);
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
    public static Method discoverFunctionalMethod(Class<?> pojoFunctionClass) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionalMethod(pojoFunctionClass);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type discoverFunctionTypeFromClass(Class<?> functionalClass) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionTypeFromClass(functionalClass);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type discoverFunctionTypeFromFunctionFactoryMethod(Class<?> clazz, String methodName) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionTypeFromFunctionFactoryMethod(clazz, methodName);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type discoverFunctionTypeFromFunctionFactoryMethod(Method method) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionTypeFromFunctionFactoryMethod(method);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type discoverFunctionTypeFromFunctionMethod(Method functionMethod) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverFunctionTypeFromFunctionMethod(functionMethod);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static int getInputCount(FunctionInvocationWrapper function) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getInputCount(function);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static int getOutputCount(FunctionInvocationWrapper function) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getOutputCount(function);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getComponentTypeOfInputType(Type functionType) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getComponentTypeOfInputType(functionType);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static Type getComponentTypeOfOutputType(Type functionType) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.getComponentTypeOfOutputType(functionType);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static String discoverBeanDefinitionNameByQualifier(ListableBeanFactory beanFactory, String qualifier) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.discoverBeanDefinitionNameByQualifier(beanFactory, qualifier);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isOutputArray(Type functionType) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isOutputArray(functionType);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isMultipleArgumentType(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isMultipleArgumentType(type);
    }

    /**
     * Delegates to our custom FunctionTypeUtils implementation.
     */
    public static boolean isJsonNode(Type type) {
        return org.activiti.cloud.common.messaging.util.FunctionTypeUtils.isJsonNode(type);
    }

    // Add any other methods that might be called by Spring Cloud Function internal code
    // All methods delegate to our custom implementation

    static {
        System.out.println("SUCCESS: Complete Shadow FunctionTypeUtils loaded - all calls will be delegated to Activiti Cloud's custom implementation");
    }
}
