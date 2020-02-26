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

import org.springframework.hateoas.PagedResources;

public class ExtendedPageMetadataConverter {

    public ExtendedPageMetadata toExtendedPageMetadata(long skipCount, PagedResources.PageMetadata basePageMetadata) {
        long totalPages = basePageMetadata.getTotalPages();
        int skipCountRemainder = Math.toIntExact(skipCount % basePageMetadata.getSize());
        if (skipCountRemainder != 0) {
            // exclude the first page, which has a different size than other pages
            int firstPageSize = skipCountRemainder;
            long totalElementsNotInTheFirstPage = basePageMetadata.getTotalElements() - firstPageSize;
            // then calculate the number of pages other than the first one and increment it by one (the first page)
            totalPages = new PagedResources.PageMetadata(basePageMetadata.getSize(),
                                                         basePageMetadata.getNumber(),
                                                         totalElementsNotInTheFirstPage).getTotalPages() + 1;
        }
        return new ExtendedPageMetadata(skipCount,
                                        basePageMetadata.getSize(),
                                        basePageMetadata.getNumber(),
                                        basePageMetadata.getTotalElements(),
                                        totalPages
        );
    }

}
