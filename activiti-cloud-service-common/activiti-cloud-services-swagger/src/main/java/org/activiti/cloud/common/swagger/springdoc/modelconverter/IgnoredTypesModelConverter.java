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
package org.activiti.cloud.common.swagger.springdoc.modelconverter;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IgnoredTypesModelConverter implements ModelConverter {

    private static final String[] IGNORED_CLASS_NAMES = {
        "com.fasterxml.jackson.databind.JavaType",
        "org.hibernate.engine.spi.EntityEntry",
        "org.hibernate.engine.spi.ManagedEntity",
        "org.hibernate.engine.spi.PersistentAttributeInterceptor"
    };

    private static final Set<Class<?>> IGNORED_CLASSES;

    static {
        IGNORED_CLASSES =
            Stream.of(IGNORED_CLASS_NAMES)
                .map(IgnoredTypesModelConverter::forName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static Optional<Class<?>> forName(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className, false, IgnoredTypesModelConverter.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {}

        return Optional.ofNullable(clazz);
    }

    // fixes NPE exception in SpringDoc SchemaPropertyDeprecatingConverter (issue #3934)
    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = Json.mapper().constructType(annotatedType.getType());
        if (javaType != null && IGNORED_CLASSES.contains(javaType.getRawClass())) {
            return null;
        }
        return (chain.hasNext()) ? chain.next().resolve(annotatedType, context, chain) : null;
    }
}
