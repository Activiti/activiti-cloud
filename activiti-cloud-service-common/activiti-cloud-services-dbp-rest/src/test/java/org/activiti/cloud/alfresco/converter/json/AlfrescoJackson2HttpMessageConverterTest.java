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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;

import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class AlfrescoJackson2HttpMessageConverterTest {

    @Spy
    @InjectMocks
    private AlfrescoJackson2HttpMessageConverter<String> httpMessageConverter;

    @Mock
    private PagedModelConverter pagedCollectionModelConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ListResponseContent<String> alfrescoPageContentListWrapper;

    @Mock
    private PagedModel<EntityModel<String>> basePagedModel;

    @Mock
    private CollectionModel<EntityModel<String>> baseCollectionModel;

    @Mock
    private Type type;

    @Mock
    private HttpOutputMessage outputMessage;

    @Captor
    private ArgumentCaptor<EntryResponseContent<String>> contentEntryArgumentCaptor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void writeInternalShouldConvertObjectUsingPagedModelConverterWhenIsAPagedModel() throws Exception {
        //given
        given(pagedCollectionModelConverter.pagedCollectionModelToListResponseContent(basePagedModel))
                .willReturn(alfrescoPageContentListWrapper);

        doNothing().when(httpMessageConverter).defaultWriteInternal(alfrescoPageContentListWrapper,
                                                                    type,
                                                                    outputMessage);

        //when
        httpMessageConverter.writeInternal(basePagedModel,
                                           type,
                                           outputMessage);

        //then
        verify(httpMessageConverter).defaultWriteInternal(alfrescoPageContentListWrapper,
                                                          type,
                                                          outputMessage);
    }

    @Test
    public void writeInternalShouldConvertWrapContentInsideAlfrescoContentEntryWhenObjectIsAGroupOfCollectionModel() throws Exception {

        //given
        given(pagedCollectionModelConverter.resourcesToListResponseContent(baseCollectionModel))
                .willReturn(alfrescoPageContentListWrapper);

        doNothing().when(httpMessageConverter).defaultWriteInternal(alfrescoPageContentListWrapper,
                type,
                outputMessage);

        //when
        httpMessageConverter.writeInternal(baseCollectionModel,
                type,
                outputMessage);

        //then
        verify(httpMessageConverter).defaultWriteInternal(alfrescoPageContentListWrapper,
                type,
                outputMessage);
    }


    @Test
    public void writeInternalShouldConvertWrapContentInsideAlfrescoContentEntryWhenObjectIsASingleResource() throws Exception {
        //given
        doNothing().when(httpMessageConverter).defaultWriteInternal(any(),
                                                                    eq(type),
                                                                    eq(outputMessage));

        //when
        httpMessageConverter.writeInternal(new EntityModel<>("content"),
                                           type,
                                           outputMessage);

        //then
        verify(httpMessageConverter).defaultWriteInternal(contentEntryArgumentCaptor.capture(),
                                                          eq(type),
                                                          eq(outputMessage));
        assertThat(contentEntryArgumentCaptor.getValue().getEntry()).isEqualTo("content");
    }

    @Test
    public void getSupportedMediaTypesShouldReturnApplicationJson() {
        //when
        List<MediaType> supportedMediaTypes = httpMessageConverter.getSupportedMediaTypes();

        //then
        assertThat(supportedMediaTypes).containsExactly(MediaType.APPLICATION_JSON);
    }

    @Test
    public void canWriteShouldFalseWhenTypeIsString() {
        //given
        Class<String> clazz = String.class;

        //when
        boolean canWrite = httpMessageConverter.canWrite(clazz, clazz, MediaType.APPLICATION_JSON);

        //then
        assertThat(canWrite).isFalse();
    }

    @Test
    public void canWriteShouldReturnTrueWhenTypeIsNotStringAndMediaTypeIsApplicationJson() {
        //given
        Class<?> clazz = EntityModel.class;
        given(httpMessageConverter.canWrite(clazz, MediaType.APPLICATION_JSON)).willReturn(true);

        //when
        boolean canWrite = httpMessageConverter.canWrite(clazz, clazz, MediaType.APPLICATION_JSON);

        //then
        assertThat(canWrite).isTrue();
    }

}
