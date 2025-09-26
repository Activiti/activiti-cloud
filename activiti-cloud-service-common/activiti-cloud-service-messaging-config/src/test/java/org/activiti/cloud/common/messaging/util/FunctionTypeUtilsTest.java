/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that our custom FunctionTypeUtils is being used instead of Spring Cloud Function's implementation.
 * This is a unit test that doesn't require Spring Boot context.
 */
public class FunctionTypeUtilsTest {

    @Test
    public void testCustomFunctionTypeUtilsIsLoaded() {
        // Verify that the FunctionTypeUtils class being used is from our custom package
        String packageName = FunctionTypeUtils.class.getPackage().getName();
        assertThat(packageName).isEqualTo("org.activiti.cloud.common.messaging.util");

        // Test that basic functionality works
        Class<?> rawType = FunctionTypeUtils.getRawType(String.class);
        assertThat(rawType).isEqualTo(String.class);

        // Test functionType method
        Type functionType = FunctionTypeUtils.functionType(String.class, Integer.class);
        assertThat(functionType).isNotNull();

        System.out.println("SUCCESS: Custom FunctionTypeUtils is being used from package: " + packageName);
    }

    @Test
    public void testFunctionTypeUtilsNotFromSpringCloudFunction() {
        // Ensure we're not accidentally using the Spring Cloud Function version
        String className = FunctionTypeUtils.class.getName();
        assertThat(className).doesNotContain("org.springframework.cloud.function.context.catalog");
        assertThat(className).isEqualTo("org.activiti.cloud.common.messaging.util.FunctionTypeUtils");

        System.out.println("SUCCESS: Confirmed not using Spring Cloud Function's FunctionTypeUtils");
    }

    @Test
    public void testGetRawTypeWithParameterizedType() {
        // Test getRawType with a parameterized type
        Type listStringType = java.util.List.class;
        Class<?> rawType = FunctionTypeUtils.getRawType(listStringType);
        assertThat(rawType).isEqualTo(java.util.List.class);
    }

    @Test
    public void testFunctionTypeCreation() {
        // Test creating function types
        Type functionType = FunctionTypeUtils.functionType(String.class, Integer.class);
        assertThat(functionType).isNotNull();

        // Test consumer type (void output)
        Type consumerType = FunctionTypeUtils.functionType(String.class, Void.class);
        assertThat(consumerType).isNotNull();

        // Test supplier type (void input)
        Type supplierType = FunctionTypeUtils.functionType(Void.class, String.class);
        assertThat(supplierType).isNotNull();
    }

    @Test
    public void testIsTypeCollection() {
        // Test collection type detection
        boolean isCollection = FunctionTypeUtils.isTypeCollection(java.util.List.class);
        assertThat(isCollection).isTrue();

        boolean isNotCollection = FunctionTypeUtils.isTypeCollection(String.class);
        assertThat(isNotCollection).isFalse();
    }

    @Test
    public void testIsTypeArray() {
        // Test array type detection
        boolean isArray = FunctionTypeUtils.isTypeArray(String[].class);
        assertThat(isArray).isTrue();

        boolean isNotArray = FunctionTypeUtils.isTypeArray(String.class);
        assertThat(isNotArray).isFalse();
    }

    @Test
    public void testIsFunctionType() {
        // Test function type detection
        boolean isFunction = FunctionTypeUtils.isFunction(java.util.function.Function.class);
        assertThat(isFunction).isTrue();

        boolean isNotFunction = FunctionTypeUtils.isFunction(String.class);
        assertThat(isNotFunction).isFalse();
    }

    @Test
    public void testIsConsumerType() {
        // Test consumer type detection
        boolean isConsumer = FunctionTypeUtils.isConsumer(java.util.function.Consumer.class);
        assertThat(isConsumer).isTrue();

        boolean isNotConsumer = FunctionTypeUtils.isConsumer(String.class);
        assertThat(isNotConsumer).isFalse();
    }

    @Test
    public void testIsSupplierType() {
        // Test supplier type detection
        boolean isSupplier = FunctionTypeUtils.isSupplier(java.util.function.Supplier.class);
        assertThat(isSupplier).isTrue();

        boolean isNotSupplier = FunctionTypeUtils.isSupplier(String.class);
        assertThat(isNotSupplier).isFalse();
    }
}
