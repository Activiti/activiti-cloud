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

package org.activiti.cloud.alfresco.argument.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AlfrescoPageArgumentMethodResolver implements PageableArgumentResolver {

    private final AlfrescoPageParameterParser pageParameterParser;
    private final PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;

    public AlfrescoPageArgumentMethodResolver(AlfrescoPageParameterParser pageParameterParser,
                                              PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver) {
        this.pageParameterParser = pageParameterParser;
        this.pageableHandlerMethodArgumentResolver = pageableHandlerMethodArgumentResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Pageable.class);
    }

    @Nullable
    @Override
    public Pageable resolveArgument(MethodParameter parameter,
                                    @Nullable ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest,
                                    @Nullable WebDataBinderFactory binderFactory) {
        Pageable basePageable = pageableHandlerMethodArgumentResolver.resolveArgument(parameter,
                                                                                      mavContainer,
                                                                                      webRequest,
                                                                                      binderFactory);

        AlfrescoQueryParameters alfrescoQueryParameters = pageParameterParser.parseParameters(webRequest);
        if (alfrescoQueryParameters.getSkipCountParameter().isSet() || alfrescoQueryParameters.getMaxItemsParameter().isSet()) {

            return new AlfrescoPageRequest(alfrescoQueryParameters.getSkipCountParameter().getValue(),
                                           alfrescoQueryParameters.getMaxItemsParameter().getValue(),
                                           basePageable);
        } else {
            return basePageable;
        }
    }
}
