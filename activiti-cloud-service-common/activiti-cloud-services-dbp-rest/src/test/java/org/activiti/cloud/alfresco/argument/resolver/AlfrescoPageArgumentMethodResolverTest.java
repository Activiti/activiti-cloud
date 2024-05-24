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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import org.activiti.test.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
public class AlfrescoPageArgumentMethodResolverTest {

    @InjectMocks
    private AlfrescoPageArgumentMethodResolver alfrescoPageArgumentMethodResolver;

    @Mock
    private AlfrescoPageParameterParser pageParameterParser;

    @Mock
    private PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;

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

    @Test
    public void resolveArgumentShouldReturnAPageableBasedOnParsedSkipCountAndMaxItems() throws Exception {
        //given
        MethodParameter methodParameter = mock(MethodParameter.class);
        ModelAndViewContainer modelAndViewContainer = mock(ModelAndViewContainer.class);

        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);

        Pageable basePageable = mock(Pageable.class);
        given(
            pageableHandlerMethodArgumentResolver.resolveArgument(
                methodParameter,
                modelAndViewContainer,
                webRequest,
                binderFactory
            )
        )
            .willReturn(basePageable);

        given(pageParameterParser.parseParameters(webRequest))
            .willReturn(
                new AlfrescoQueryParameters(
                    new SkipCountParameter(true, 40L),
                    new MaxItemsParameter(true, 20),
                    new PageParameter(true, 20)
                )
            );

        //when
        Pageable resolvedPageable = alfrescoPageArgumentMethodResolver.resolveArgument(
            methodParameter,
            modelAndViewContainer,
            webRequest,
            binderFactory
        );

        //then
        assertThat(resolvedPageable).isInstanceOf(AlfrescoPageRequest.class);
        Assertions
            .assertThat((AlfrescoPageRequest) resolvedPageable)
            .hasOffset(40)
            .hasPageSize(20)
            .hasPageable(basePageable);
    }
}
