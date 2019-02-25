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

import java.util.Collections;
import java.util.List;

import org.activiti.cloud.alfresco.rest.model.AlfrescoContentEntry;
import org.activiti.cloud.alfresco.rest.model.AlfrescoPageContentListWrapper;
import org.activiti.cloud.alfresco.rest.model.AlfrescoPageMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class PagedResourcesConverterTest {

    @InjectMocks
    private PagedResourcesConverter pagedResourcesConverter;

    @Mock
    private PageMetadataConverter pageMetadataConverter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void toAlfrescoContentListWrapperShouldConvertFromPagedResourcesToAlfrescoContentListWrapper() throws Exception {
        //given
        List<Resource<String>> elements = Collections.singletonList(new Resource<>("any"));
        PagedResources.PageMetadata basePageMetaData = new PagedResources.PageMetadata(10,
                                                                               1,
                                                                               100);

        AlfrescoPageMetadata alfrescoPageMetadata = new AlfrescoPageMetadata();
        given(pageMetadataConverter.toAlfrescoPageMetadata(basePageMetaData, elements.size())).willReturn(alfrescoPageMetadata);

        //when
        AlfrescoPageContentListWrapper<String> alfrescoPageContentListWrapper = pagedResourcesConverter.toAlfrescoContentListWrapper(new PagedResources<>(elements,
                                                                                                                                                          basePageMetaData));

        //then
        assertThat(alfrescoPageContentListWrapper).isNotNull();
        assertThat(alfrescoPageContentListWrapper.getList().getEntries())
                .extracting(AlfrescoContentEntry::getEntry)
                .containsExactly("any");
        assertThat(alfrescoPageContentListWrapper.getList().getPagination()).isEqualTo(alfrescoPageMetadata);

    }

    @Test
    public void toAlfrescoContentListWrapperShouldConvertFromResourcesToAlfrescoContentListWrapper() throws Exception {
        //given
        List<Resource<String>> elements = Collections.singletonList(new Resource<>("any"));

        //when
        AlfrescoPageContentListWrapper<String> alfrescoPageContentListWrapper = pagedResourcesConverter.toAlfrescoContentListWrapper(new Resources<>(elements));

        //then
        assertThat(alfrescoPageContentListWrapper).isNotNull();
        assertThat(alfrescoPageContentListWrapper.getList().getEntries())
                .extracting(AlfrescoContentEntry::getEntry)
                .containsExactly("any");
    }

}