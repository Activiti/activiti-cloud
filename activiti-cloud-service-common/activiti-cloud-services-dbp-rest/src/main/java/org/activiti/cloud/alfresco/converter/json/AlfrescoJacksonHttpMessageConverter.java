/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.alfresco.converter.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

public class AlfrescoJacksonHttpMessageConverter<T> extends JacksonJsonHttpMessageConverter {

    private final PagedModelConverter pagedCollectionModelConverter;

    public AlfrescoJacksonHttpMessageConverter(
        PagedModelConverter pagedCollectionModelConverter,
        JsonMapper jsonMapper
    ) {
        super(jsonMapper);
        this.pagedCollectionModelConverter = pagedCollectionModelConverter;
        setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Override
    protected void writeInternal(
        Object object,
        @Nullable ResolvableType type,
        HttpOutputMessage outputMessage,
        Map<String, Object> hints
    ) throws IOException, HttpMessageNotWritableException {
        super.writeInternal(transformObject(object), type, outputMessage, hints);
    }

    @SuppressWarnings("unchecked")
    private Object transformObject(Object object) {
        if (object instanceof PagedModel) {
            return pagedCollectionModelConverter.pagedCollectionModelToListResponseContent(
                (PagedModel<EntityModel<T>>) object
            );
        } else if (object instanceof CollectionModel) {
            return pagedCollectionModelConverter.resourcesToListResponseContent(
                (CollectionModel<EntityModel<T>>) object
            );
        } else if (object instanceof EntityModel) {
            return new EntryResponseContent<>(((EntityModel<T>) object).getContent());
        }
        return object;
    }

    @Override
    public boolean canWrite(ResolvableType type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
        return !String.class.equals(type.toClass()) && super.canWrite(type, clazz, mediaType);
    }
}
