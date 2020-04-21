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

import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.activiti.cloud.alfresco.rest.model.PaginationMetadata;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;

public class PagedModelConverter {

    private PageMetadataConverter pageMetadataConverter;

    public PagedModelConverter(PageMetadataConverter pageMetadataConverter) {
        this.pageMetadataConverter = pageMetadataConverter;
    }

    public <T> ListResponseContent<T> pagedCollectionModelToListResponseContent(PagedModel<EntityModel<T>> pagedCollectionModel) {
        List<EntryResponseContent<T>> baseContent = getAlfrescoContentEntries(pagedCollectionModel);

        PaginationMetadata pagination = pageMetadataConverter.toAlfrescoPageMetadata(pagedCollectionModel.getMetadata(),
                                                                                     baseContent.size());

        return ListResponseContent.wrap(baseContent,
                                        pagination);
    }

    public <T> ListResponseContent<T> resourcesToListResponseContent(CollectionModel<EntityModel<T>> resources) {

        return ListResponseContent.wrap(getAlfrescoContentEntries(resources),
                                        null);
    }

    private <T> List<EntryResponseContent<T>> getAlfrescoContentEntries(CollectionModel<EntityModel<T>> pagedCollectionModel) {
        Collection<EntityModel<T>> pagedResourceContent = pagedCollectionModel.getContent();
        return pagedResourceContent.stream()
                .map(
                        resource -> new EntryResponseContent<>(resource.getContent())
                ).collect(Collectors.toList());
    }
}
