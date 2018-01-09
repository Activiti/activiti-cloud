/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.organization.core.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Reflection related utilities.
 */
public final class ReflectionUtils {

    /**
     * Get entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @param exceptionMessage message exception supplier
     * @return the field value
     */
    public static Object getFieldValue(Object entity,
                                       String fieldName,
                                       Supplier<String> exceptionMessage) {
        try {
            return new PropertyDescriptor(fieldName,
                                          entity.getClass())
                    .getReadMethod()
                    .invoke(entity);
        } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
            throw new RuntimeException(exceptionMessage.get(),
                                       e);
        }
    }

    /**
     * Set entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @param exceptionMessage message exception supplier
     * @return the field value
     */
    public static void setFieldValue(Object entity,
                                     String fieldName,
                                     Object value,
                                     Supplier<String> exceptionMessage) {
        try {
            new PropertyDescriptor(fieldName,
                                   entity.getClass())
                    .getWriteMethod()
                    .invoke(entity,
                            value);
        } catch (IllegalAccessException | IntrospectionException | InvocationTargetException e) {
            throw new RuntimeException(exceptionMessage.get(),
                                       e);
        }
    }

    /**
     * Get entity field type.
     * @param entity the entity
     * @param fieldName the entity field name
     * @param exceptionMessage message exception supplier
     * @return the field type
     */
    public static Class<?> getFieldClass(Object entity,
                                         String fieldName,
                                         Supplier<String> exceptionMessage) {
        try {
            return entity.getClass().getDeclaredField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(exceptionMessage.get(),
                                       e);
        }
    }

    private ReflectionUtils() {

    }
}
