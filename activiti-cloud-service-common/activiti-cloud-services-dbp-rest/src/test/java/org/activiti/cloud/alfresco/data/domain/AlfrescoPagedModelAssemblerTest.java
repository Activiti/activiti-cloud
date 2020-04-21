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

import java.util.Collections;

import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.RepresentationModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class AlfrescoPagedModelAssemblerTest {

    @Spy
    @InjectMocks
    private AlfrescoPagedModelAssembler<String> alfrescoPagedModelAssembler;

    @Mock
    private ExtendedPageMetadataConverter extendedPageMetadataConverter;

    @Mock
    private RepresentationModelAssembler<String, RepresentationModel<?>> resourceAssembler;

    @Mock
    private Page<String> page;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReplaceBasePageMetadataByExtendedPageMetadata() throws Exception {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(3,
                                                                          10,
                                                                          null);

        PagedModel.PageMetadata baseMetadata = new PagedModel.PageMetadata(10,
                                                                               0,
                                                                               30);
        RepresentationModel resourceSupport = new RepresentationModel();
        Link link = mock(Link.class);
        PagedModel<RepresentationModel> basePagedModel = new PagedModel<>(Collections.singletonList(resourceSupport),
                                                                                        baseMetadata,
                                                                                        link);

        doReturn(basePagedModel).when(alfrescoPagedModelAssembler).toModel(page,
                                                                                      resourceAssembler);
        ExtendedPageMetadata extendedPageMetadata = mock(ExtendedPageMetadata.class);
        given(extendedPageMetadataConverter.toExtendedPageMetadata(alfrescoPageRequest.getOffset(), baseMetadata)).willReturn(extendedPageMetadata);

        //when
        PagedModel<RepresentationModel<?>> pagedCollectionModel = alfrescoPagedModelAssembler.toModel(alfrescoPageRequest,
                                                                                                    page,
                                                                                                    resourceAssembler);

        //then
        assertThat(pagedCollectionModel).isNotNull();
        assertThat(pagedCollectionModel.getMetadata()).isEqualTo(extendedPageMetadata);
        assertThat(pagedCollectionModel.getContent()).containsExactly(resourceSupport);
        assertThat(pagedCollectionModel.getLinks()).contains(link);
    }

}
