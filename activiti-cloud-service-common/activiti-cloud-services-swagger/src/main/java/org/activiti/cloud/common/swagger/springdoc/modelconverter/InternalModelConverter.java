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
import com.fasterxml.jackson.databind.type.ResolvedRecursiveType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Iterator;
import java.util.List;

public abstract class InternalModelConverter<T> implements ModelConverter {

    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = Json.mapper().constructType(annotatedType.getType());
        if (javaType != null) {
            if (applies(javaType)) {
                ResolvedRecursiveType resolvedRecursiveType = resolveType(javaType);
                annotatedType =
                    new AnnotatedType()
                        .type(resolvedRecursiveType)
                        .ctxAnnotations(annotatedType.getCtxAnnotations())
                        .parent(annotatedType.getParent())
                        .schemaProperty(annotatedType.isSchemaProperty())
                        .resolveAsRef(annotatedType.isResolveAsRef())
                        .jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
                        .propertyName(annotatedType.getPropertyName())
                        .skipOverride(true);
                return this.resolve(annotatedType, context, chain);
            }
        }
        return (chain.hasNext()) ? chain.next().resolve(annotatedType, context, chain) : null;
    }

    protected abstract boolean applies(JavaType javaType);

    protected abstract Class<T> getAlternateTypeClass();

    protected abstract JavaType getContainedType(JavaType javaType);

    private ResolvedRecursiveType resolveType(JavaType javaType) {
        return new ResolvedRecursiveType(
            getAlternateTypeClass(),
            TypeBindings.create(getAlternateTypeClass(), List.of(getContainedType(javaType)))
        );
    }
}
