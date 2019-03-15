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

import org.springframework.web.context.request.NativeWebRequest;

public class AlfrescoPageParameterParser {

    private final int defaultPageSize;

    public AlfrescoPageParameterParser(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public AlfrescoQueryParameters parseParameters(NativeWebRequest webRequest) {
        return new AlfrescoQueryParameters(parseSkipCount(webRequest), parseMaxItems(webRequest));
    }

    protected MaxItemsParameter parseMaxItems(NativeWebRequest webRequest) {
        int maxItems = defaultPageSize;

        String maxItemsString = webRequest.getParameter("maxItems");
        boolean isSet = maxItemsString != null;
        if (isSet) {
            maxItems = Integer.parseInt(maxItemsString);
        }

        return new MaxItemsParameter(isSet, maxItems);
    }

    protected SkipCountParameter parseSkipCount(NativeWebRequest webRequest) {
        long skipCount = 0;
        String skipCountString = webRequest.getParameter("skipCount");
        boolean isSet = skipCountString != null;
        if (isSet) {
            skipCount = Long.parseLong(skipCountString);
        }
        return new SkipCountParameter(isSet, skipCount);
    }

}
