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

package org.activiti.cloud.alfresco.data.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponents;

public class AlfrescoPagedResourcesAssembler<T> extends PagedResourcesAssembler<T> {

    private final ExtendedPageMetadataConverter extendedPageMetadataConverter;

    /**
     * Creates a new {@link PagedResourcesAssembler} using the given {@link PageableHandlerMethodArgumentResolver} and
     * base URI. If the former is {@literal null}, a default one will be created. If the latter is {@literal null}, calls
     * to {@link #toResource(Page)} will use the current request's URI to build the relevant previous and next links.
     * @param resolver can be {@literal null}.
     * @param baseUri can be {@literal null}.
     */
    public AlfrescoPagedResourcesAssembler(@Nullable HateoasPageableHandlerMethodArgumentResolver resolver,
                                           @Nullable UriComponents baseUri,
                                           ExtendedPageMetadataConverter extendedPageMetadataConverter) {
        super(resolver,
              baseUri);
        this.extendedPageMetadataConverter = extendedPageMetadataConverter == null ? new ExtendedPageMetadataConverter() : extendedPageMetadataConverter;
    }

    public <R extends ResourceSupport> PagedResources<R> toResource(Pageable pageable,
                                                                    Page<T> page,
                                                                    ResourceAssembler<T, R> assembler) {
        PagedResources<R> pagedResources = toResource(page,
                                                            assembler);
        ExtendedPageMetadata extendedPageMetadata = extendedPageMetadataConverter.toExtendedPageMetadata(pageable.getOffset(),
                                                                                                         pagedResources.getMetadata());
        pagedResources = new PagedResources<>(pagedResources.getContent(),
                                              extendedPageMetadata,
                                              pagedResources.getLinks());

        return pagedResources;
    }
}
