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

    private final int maxItemsLimit;
    private final boolean maxItemsLimitEnabled;

    public AlfrescoPageArgumentMethodResolver(
        AlfrescoPageParameterParser pageParameterParser,
        PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver,
        int maxItemsLimit,
        boolean maxItemsLimitEnabled
    ) {
        this.pageParameterParser = pageParameterParser;
        this.pageableHandlerMethodArgumentResolver = pageableHandlerMethodArgumentResolver;
        this.maxItemsLimit = maxItemsLimit;
        this.maxItemsLimitEnabled = maxItemsLimitEnabled;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Pageable.class);
    }

    @Nullable
    @Override
    public Pageable resolveArgument(
        MethodParameter parameter,
        @Nullable ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) {
        Pageable basePageable = pageableHandlerMethodArgumentResolver.resolveArgument(
            parameter,
            mavContainer,
            webRequest,
            binderFactory
        );

        AlfrescoQueryParameters alfrescoQueryParameters = pageParameterParser.parseParameters(webRequest);

        if (isPaginationValueExceedinglimit(alfrescoQueryParameters, basePageable)) {
            throw new IllegalStateException("Exceeded max limit of 1000 elements");
        } else if (
            alfrescoQueryParameters.getSkipCountParameter().isSet() ||
                alfrescoQueryParameters.getMaxItemsParameter().isSet()
        ) {
            return new AlfrescoPageRequest(
                alfrescoQueryParameters.getSkipCountParameter().getValue(),
                alfrescoQueryParameters.getMaxItemsParameter().getValue(),
                basePageable
            );
        } else {
            return basePageable;
        }
    }

    private boolean isPaginationValueExceedinglimit(
        AlfrescoQueryParameters alfrescoQueryParameters,
        Pageable basePageable
    ) {
        if (maxItemsLimitEnabled) {
            if (
                alfrescoQueryParameters.getMaxItemsParameter().isSet() &&
                    alfrescoQueryParameters.getMaxItemsParameter().getValue() > maxItemsLimit
            ) {
                return true;
            }
            if (
                alfrescoQueryParameters.getPageParameter().isSet() &&
                    alfrescoQueryParameters.getPageParameter().getValue() > maxItemsLimit
            ) {
                return true;
            } else if (
                alfrescoQueryParameters.getMaxItemsParameter().isSet() &&
                    alfrescoQueryParameters.getMaxItemsParameter().getValue() < maxItemsLimit
            ) {
                return false;
            } else if (basePageable.getPageSize() < maxItemsLimit) {
                return false;
            }
            return true;
        }
        return false;
        // Se ho impostato maxItems e questo supera il limite devo ritornare true
        // Se non ho impostato maxItems ma ho impostato il base pageable ed è superiore al limite torno true
        //        return (
        //            maxItemsLimitEnabled &&
        //            (
        //                !alfrescoQueryParameters.getMaxItemsParameter().isSet() ||
        //                alfrescoQueryParameters.getMaxItemsParameter().getValue() > maxItemsLimit
        //            )
        //        );
    }
}
