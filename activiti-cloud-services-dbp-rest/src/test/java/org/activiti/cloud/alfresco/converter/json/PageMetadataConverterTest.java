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
import org.junit.Test;
import org.springframework.hateoas.PagedResources;

import static org.activiti.test.Assertions.assertThat;

public class PageMetadataConverterTest {

    private PageMetadataConverter converter = new PageMetadataConverter();

    @Test
    public void toAlfrescoPageMetadataShouldCalculateAlfrescoMetadataFromBaseMetadata() {
        //given
        PagedResources.PageMetadata basePageMetadata = new PagedResources.PageMetadata(10,
                                                                                       2,
                                                                                       100);

        //when
        PaginationMetadata alfrescoPageMetadata = converter.toAlfrescoPageMetadata(basePageMetadata,
                                                                                   10);

        //then
        assertThat(alfrescoPageMetadata)
                .hasCount(10)
                .hasMaxItems(10) // same as page size
                .hasSkipCount(20) // current page is 2, so pages 0 and 1 was skipped: 20 elements
                .hasTotalItems(100)
                .isHasMoreItems();
    }

    @Test
    public void toAlfrescoPageMetadataShouldReturnMetadataWithNoMoreItemsWhenIsInTheLastPage() {
        //given
        PagedResources.PageMetadata basePageMetadata = new PagedResources.PageMetadata(10,
                                                                                       1,
                                                                                       11);

        //when
        PaginationMetadata alfrescoPageMetadata = converter.toAlfrescoPageMetadata(basePageMetadata,
                                                                                   10);

        //then
        assertThat(alfrescoPageMetadata)
                .isNotHasMoreItems();
    }

    @Test
    public void toAlfrescoPageMetadataShouldUseSkipCountFromExtendedPageMetadataWhenAvailable() {
        //given
        ExtendedPageMetadata baseMetadata = new ExtendedPageMetadata(3,
                                                                     10,
                                                                     1,
                                                                     8,
                                                                     2);

        //when
        PaginationMetadata alfrescoPageMetadata = converter.toAlfrescoPageMetadata(baseMetadata,
                                                                                   5);

        //then
        assertThat(alfrescoPageMetadata).hasSkipCount(3);
    }

}