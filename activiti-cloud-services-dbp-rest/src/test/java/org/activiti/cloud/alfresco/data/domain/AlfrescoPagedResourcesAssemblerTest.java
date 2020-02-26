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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class AlfrescoPagedResourcesAssemblerTest {

    @Spy
    @InjectMocks
    private AlfrescoPagedResourcesAssembler<String> alfrescoPagedResourcesAssembler;

    @Mock
    private ExtendedPageMetadataConverter extendedPageMetadataConverter;

    @Mock
    private ResourceAssembler<String, ResourceSupport> resourceAssembler;

    @Mock
    private Page<String> page;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReplaceBasePageMetadataByExtendedPageMetadata() throws Exception {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(3,
                                                                          10,
                                                                          null);

        PagedResources.PageMetadata baseMetadata = new PagedResources.PageMetadata(10,
                                                                               0,
                                                                               30);
        ResourceSupport resourceSupport = new ResourceSupport();
        Link link = mock(Link.class);
        PagedResources<ResourceSupport> basePagedResources = new PagedResources<>(Collections.singletonList(resourceSupport),
                                                                                        baseMetadata,
                                                                                        link);

        doReturn(basePagedResources).when(alfrescoPagedResourcesAssembler).toResource(page,
                                                                                      resourceAssembler);
        ExtendedPageMetadata extendedPageMetadata = mock(ExtendedPageMetadata.class);
        given(extendedPageMetadataConverter.toExtendedPageMetadata(alfrescoPageRequest.getOffset(), baseMetadata)).willReturn(extendedPageMetadata);

        //when
        PagedResources<ResourceSupport> pagedResources = alfrescoPagedResourcesAssembler.toResource(alfrescoPageRequest,
                                                                                                    page,
                                                                                                    resourceAssembler);

        //then
        assertThat(pagedResources).isNotNull();
        assertThat(pagedResources.getMetadata()).isEqualTo(extendedPageMetadata);
        assertThat(pagedResources.getContent()).containsExactly(resourceSupport);
        assertThat(pagedResources.getLinks()).contains(link);
    }

}