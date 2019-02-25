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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.cloud.alfresco.rest.model.AlfrescoContentEntry;
import org.activiti.cloud.alfresco.rest.model.AlfrescoPageContentListWrapper;
import org.activiti.cloud.alfresco.rest.model.AlfrescoPageMetadata;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Component;

@Component
public class PagedResourcesConverter {

    private PageMetadataConverter pageMetadataConverter;

    public PagedResourcesConverter(PageMetadataConverter pageMetadataConverter) {
        this.pageMetadataConverter = pageMetadataConverter;
    }

    public <T> AlfrescoPageContentListWrapper<T> toAlfrescoContentListWrapper(PagedResources<Resource<T>> pagedResources) {
        List<AlfrescoContentEntry<T>> baseContent = getAlfrescoContentEntries(pagedResources);

        AlfrescoPageMetadata pagination = pageMetadataConverter.toAlfrescoPageMetadata(pagedResources.getMetadata(),
                baseContent.size());

        return AlfrescoPageContentListWrapper.wrap(baseContent,
                pagination);
    }

    public <T> AlfrescoPageContentListWrapper<T> toAlfrescoContentListWrapper(Resources<Resource<T>> pagedResources) {
        List<AlfrescoContentEntry<T>> baseContent = getAlfrescoContentEntries(pagedResources);

        return AlfrescoPageContentListWrapper.wrap(baseContent,
                null);
    }

    private <T> List<AlfrescoContentEntry<T>> getAlfrescoContentEntries(Resources<Resource<T>> pagedResources) {
        Collection<Resource<T>> pagedResourceContent = pagedResources.getContent();
        return pagedResourceContent.stream()
                .map(
                        resource -> new AlfrescoContentEntry<>(resource.getContent())
                ).collect(Collectors.toList());
    }
}
