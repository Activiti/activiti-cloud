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

package org.activiti.cloud.organization.core.util;

import java.beans.PropertyDescriptor;
import java.util.function.Supplier;

/**
 * Reflection related utilities.
 */
public final class ReflectionUtils {

    /**
     * Get entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @return the field value
     */
    public static <T> T getFieldValue(Object entity,
                                      String fieldName) {
        return getFieldValue(entity,
                             fieldName,
                             () -> String.format(
                                     "Cannot access field '%s' of entity type '%s'",
                                     fieldName,
                                     entity.getClass()));
    }

    /**
     * Get entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @param exceptionMessage message exception supplier
     * @return the field value
     */
    public static <T> T getFieldValue(Object entity,
                                      String fieldName,
                                      Supplier<String> exceptionMessage) {
        try {
            return (T) new PropertyDescriptor(fieldName,
                                              entity.getClass())
                    .getReadMethod()
                    .invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException(exceptionMessage.get(),
                                       e);
        }
    }

    /**
     * Set entity field value.
     * @param entity the entity
     * @param fieldName the entity field name
     * @return the field value
     */
    public static void setFieldValue(Object entity,
                                     String fieldName,
                                     Object value) {
        setFieldValue(entity,
                      fieldName,
                      value,
                      () -> String.format(
                              "Cannot set value to the target field '%s' of entity type '%s'",
                              fieldName,
                              entity.getClass()));
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
        } catch (Exception e) {
            throw new RuntimeException(exceptionMessage.get(),
                                       e);
        }
    }

    private ReflectionUtils() {

    }
}
