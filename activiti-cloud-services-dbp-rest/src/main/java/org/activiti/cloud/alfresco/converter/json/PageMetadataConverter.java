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

import org.activiti.cloud.alfresco.data.domain.ExtendedPageMetadata;
import org.activiti.cloud.alfresco.rest.model.PaginationMetadata;
import org.springframework.hateoas.PagedResources;

public class PageMetadataConverter {

    public PaginationMetadata toAlfrescoPageMetadata(PagedResources.PageMetadata basePageMetadata,
                                                     long count) {
        long skipCount = basePageMetadata.getNumber() * basePageMetadata.getSize();
        if (basePageMetadata instanceof ExtendedPageMetadata) {
            skipCount = ((ExtendedPageMetadata) basePageMetadata).getSkipCount();
        }

        // the page number starts from zero, so it's necessary to increment by one before comparing with total pages
        return new PaginationMetadata(skipCount,
                                      basePageMetadata.getSize(),
                                      count,
                                      basePageMetadata.getTotalPages() > basePageMetadata.getNumber() + 1,
                                      basePageMetadata.getTotalElements());
    }
}
