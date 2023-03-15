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

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.NativeWebRequest;

public class AlfrescoPageRequestParameterParserTest {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String MAX_ITEMS_KEY = "maxItems";
    private static final String SKIP_COUNT_KEY = "skipCount";

    private AlfrescoPageParameterParser pageParameterParser = new AlfrescoPageParameterParser(DEFAULT_PAGE_SIZE);

    @Test
    public void parseMaxItemsShouldReturnValueOfMaxItemsParameter() throws Exception {
        //given
        NativeWebRequest webRequest = buildRequest(MAX_ITEMS_KEY, "50");

        //when
        MaxItemsParameter maxItems = pageParameterParser.parseMaxItems(webRequest);

        //then
        assertThat(maxItems).isSet().hasValue(50);
    }

    @Test
    public void parseMaxItemsShouldReturnDefaultPageSizeWhenMaxItemsIsNotSet() throws Exception {
        //given
        NativeWebRequest webRequest = mock(NativeWebRequest.class);

        //when
        MaxItemsParameter maxItems = pageParameterParser.parseMaxItems(webRequest);

        //then
        assertThat(maxItems).isNotSet().hasValue(DEFAULT_PAGE_SIZE);
    }

    private NativeWebRequest buildRequest(String paramName, String paramValue) {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        given(webRequest.getParameter(paramName)).willReturn(paramValue);
        return webRequest;
    }

    @Test
    public void parseSkipCountShouldReturnValueOfSkipCountParameter() throws Exception {
        //given
        NativeWebRequest request = buildRequest(SKIP_COUNT_KEY, "200");

        //when
        SkipCountParameter skipCount = pageParameterParser.parseSkipCount(request);

        //then
        assertThat(skipCount).isSet().hasValue(200L);
    }

    @Test
    public void parseSkipCountShouldReturnZeroWhenSkipCountIsNotSet() throws Exception {
        //given
        NativeWebRequest request = mock(NativeWebRequest.class);

        //when
        SkipCountParameter skipCount = pageParameterParser.parseSkipCount(request);

        //then
        assertThat(skipCount).isNotSet().hasValue(0L);
    }

    @Test
    public void parseParametersShouldParseSkipCountAndMaxItems() throws Exception {
        //given
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        given(webRequest.getParameter(SKIP_COUNT_KEY)).willReturn("30");
        given(webRequest.getParameter(MAX_ITEMS_KEY)).willReturn("10");

        //when
        AlfrescoQueryParameters alfrescoQueryParameters = pageParameterParser.parseParameters(webRequest);

        //then
        assertThat(alfrescoQueryParameters.getSkipCountParameter()).isNotNull().isSet().hasValue(30L);
        assertThat(alfrescoQueryParameters.getMaxItemsParameter()).isNotNull().isSet().hasValue(10);
    }
}
