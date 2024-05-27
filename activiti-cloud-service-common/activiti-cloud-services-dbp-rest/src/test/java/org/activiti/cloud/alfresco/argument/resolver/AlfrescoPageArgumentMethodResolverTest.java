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
package org.activiti.cloud.alfresco.argument.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
public class AlfrescoPageArgumentMethodResolverTest {

    private AlfrescoPageArgumentMethodResolver alfrescoPageArgumentMethodResolver;

    @Mock
    private AlfrescoPageParameterParser pageParameterParser;

    @Mock
    private PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;

    @BeforeEach
    public void setUp() {
        alfrescoPageArgumentMethodResolver =
            new AlfrescoPageArgumentMethodResolver(
                new AlfrescoPageParameterParser(1000),
                pageableHandlerMethodArgumentResolver,
                1000,
                true
            );
    }

    @Test
    public void supportsParameterShouldReturnTrueWhenItsAPageable() throws Exception {
        //given
        MethodParameter parameter = buildParameter(Pageable.class);

        //when
        boolean supportsParameter = alfrescoPageArgumentMethodResolver.supportsParameter(parameter);

        //then
        assertThat(supportsParameter).isTrue();
    }

    @Test
    public void supportsParameterShouldReturnFalseWhenItsNotAPageable() throws Exception {
        //given
        MethodParameter parameter = buildParameter(String.class);

        //when
        boolean supportsParameter = alfrescoPageArgumentMethodResolver.supportsParameter(parameter);

        //then
        assertThat(supportsParameter).isFalse();
    }

    private MethodParameter buildParameter(Class<?> parameterType) {
        MethodParameter parameter = mock(MethodParameter.class);
        doReturn(parameterType).when(parameter).getParameterType();
        return parameter;
    }
}
