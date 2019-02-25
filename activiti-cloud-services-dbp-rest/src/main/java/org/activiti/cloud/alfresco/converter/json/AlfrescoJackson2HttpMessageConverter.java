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

package org.activiti.cloud.alfresco.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;

import org.activiti.cloud.alfresco.rest.model.AlfrescoContentEntry;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AlfrescoJackson2HttpMessageConverter<T> extends MappingJackson2HttpMessageConverter {

    private final PagedResourcesConverter pagedResourcesConverter;

    public AlfrescoJackson2HttpMessageConverter(PagedResourcesConverter pagedResourcesConverter) {
        this.pagedResourcesConverter = pagedResourcesConverter;
        setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Override
    protected void writeInternal(Object object,
                                 @Nullable Type type,
                                 HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Object transformedObject = object;
        if (object instanceof PagedResources) {
            transformedObject = pagedResourcesConverter.toAlfrescoContentListWrapper((PagedResources<Resource<T>>) object);
        }
        else if (object instanceof Resources){
            transformedObject = pagedResourcesConverter.toAlfrescoContentListWrapper((Resources<Resource<T>>) object);
        }
        else if (object instanceof Resource) {
            transformedObject = new AlfrescoContentEntry<>(((Resource<T>) object).getContent());
        }
        defaultWriteInternal(transformedObject,
                             type,
                             outputMessage);
    }

    protected void defaultWriteInternal(Object object,
                                      @Nullable Type type,
                                      HttpOutputMessage outputMessage) throws IOException {
        super.writeInternal(object,
                            type,
                            outputMessage);
    }

    @Override
    public boolean canWrite(Type type,
                            Class<?> clazz,
                            MediaType mediaType) {
        return !String.class.equals(type) && super.canWrite(type,
                                                            clazz,
                                                            mediaType);
    }

}
