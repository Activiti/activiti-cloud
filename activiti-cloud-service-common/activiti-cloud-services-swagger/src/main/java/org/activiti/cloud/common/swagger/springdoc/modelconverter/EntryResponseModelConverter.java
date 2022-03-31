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
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;

import java.util.Iterator;

public class EntryResponseModelConverter extends InternalModelConverter<EntryResponseContent> {

    @Override
    protected boolean applies(JavaType javaType) {
        return javaType instanceof SimpleType && EntryResponseContent.class.equals(javaType.getRawClass());
    }

    @Override
    protected Class<EntryResponseContent> getAlternateTypeClass() {
        return EntryResponseContent.class;
    }

    @Override
    protected JavaType getContainedType(JavaType javaType) {
        return javaType.containedType(0);
    }

    @Override
    protected Schema resolveWhenApplied(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        return super.resolveNext(annotatedType, context, chain);
    }
}
