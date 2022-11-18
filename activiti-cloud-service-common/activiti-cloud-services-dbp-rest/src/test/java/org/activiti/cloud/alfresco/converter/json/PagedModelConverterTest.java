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
package org.activiti.cloud.alfresco.converter.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.List;
import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.activiti.cloud.alfresco.rest.model.PaginationMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@ExtendWith(MockitoExtension.class)
public class PagedModelConverterTest {

    @InjectMocks
    private PagedModelConverter pagedCollectionModelConverter;

    @Mock
    private PageMetadataConverter pageMetadataConverter;

    @Test
    public void toAlfrescoContentListWrapperShouldConvertFromPagedModelToAlfrescoContentListWrapper() {
        //given
        List<EntityModel<String>> elements = Collections.singletonList(EntityModel.of("any"));
        PagedModel.PageMetadata basePageMetaData = new PagedModel.PageMetadata(10, 1, 100);

        PaginationMetadata alfrescoPageMetadata = new PaginationMetadata();
        given(pageMetadataConverter.toAlfrescoPageMetadata(basePageMetaData, elements.size()))
            .willReturn(alfrescoPageMetadata);

        //when
        ListResponseContent<String> alfrescoPageContentListWrapper = pagedCollectionModelConverter.pagedCollectionModelToListResponseContent(
            PagedModel.of(elements, basePageMetaData)
        );

        //then
        assertThat(alfrescoPageContentListWrapper).isNotNull();
        assertThat(alfrescoPageContentListWrapper.getList().getEntries())
            .extracting(EntryResponseContent::getEntry)
            .containsExactly("any");
        assertThat(alfrescoPageContentListWrapper.getList().getPagination()).isEqualTo(alfrescoPageMetadata);
    }

    @Test
    public void toAlfrescoContentListWrapperShouldConvertFromCollectionModelToAlfrescoContentListWrapper() {
        //given
        List<EntityModel<String>> elements = Collections.singletonList(EntityModel.of("any"));

        //when
        ListResponseContent<String> alfrescoPageContentListWrapper = pagedCollectionModelConverter.resourcesToListResponseContent(
            CollectionModel.of(elements)
        );

        //then
        assertThat(alfrescoPageContentListWrapper).isNotNull();
        assertThat(alfrescoPageContentListWrapper.getList().getEntries())
            .extracting(EntryResponseContent::getEntry)
            .containsExactly("any");
    }
}
