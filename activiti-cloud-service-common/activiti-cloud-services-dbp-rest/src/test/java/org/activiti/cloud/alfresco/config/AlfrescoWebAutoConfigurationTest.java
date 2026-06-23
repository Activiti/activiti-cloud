/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.alfresco.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageArgumentMethodResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
public class AlfrescoWebAutoConfigurationTest {

    private AlfrescoWebAutoConfiguration configurer;

    @Mock
    private PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;

    @BeforeEach
    public void setUp() {
        configurer = new AlfrescoWebAutoConfiguration(pageableHandlerMethodArgumentResolver, 100, 1000, true);
    }

    @Test
    public void addArgumentResolversShouldAddAlfrescoPageArgumentMethodResolverAtTheFirstPosition() {
        //given
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        resolvers.add(mock(HandlerMethodArgumentResolver.class));

        //when
        configurer.addArgumentResolvers(resolvers);

        //then
        assertThat(resolvers.getFirst()).isInstanceOf(AlfrescoPageArgumentMethodResolver.class);
    }

    @Test
    public void extendMessageConvertersShouldRemoveApplicationJsonFromHalConverter() {
        //given

        //when
        TypeConstrainedJacksonJsonHttpMessageConverter halConverter =
            new TypeConstrainedJacksonJsonHttpMessageConverter(EntityModel.class);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON));
        configurer.extendMessageConverters(Collections.singletonList(halConverter));

        //then
        assertThat(halConverter.getSupportedMediaTypes()).containsExactly(MediaTypes.HAL_JSON);
    }
}
