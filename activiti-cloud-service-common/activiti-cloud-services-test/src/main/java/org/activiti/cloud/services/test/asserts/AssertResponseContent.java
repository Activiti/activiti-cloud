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

package org.activiti.cloud.services.test.asserts;

import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.activiti.cloud.services.common.file.FileContent;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * Asserts for response content
 */
public class AssertResponseContent {

    private static final String ATTACHMENT_CONTENT_DISPOSITION = "attachment;filename=";

    private final MockHttpServletResponse response;

    public AssertResponseContent(MockHttpServletResponse response) {
        this.response = response;
    }

    public AssertFileContent isFile() {
        assertThat(response).isNotNull();
        assertThat(response.getContentType()).isNotNull();
        assertThat(response.getContentAsByteArray()).isNotEmpty();
        assertThat(response.getHeader(CONTENT_DISPOSITION))
                .isNotEmpty()
                .startsWith(ATTACHMENT_CONTENT_DISPOSITION);

        String filename = response.getHeader(CONTENT_DISPOSITION)
                .substring(ATTACHMENT_CONTENT_DISPOSITION.length());

        return new AssertFileContent(new FileContent(filename,
                                                     response.getContentType(),
                                                     response.getContentAsByteArray()));
    }

    public static AssertResponseContent assertThatResponseContent(MvcResult mvcResult) {
        return assertThatResponseContent(mvcResult.getResponse());
    }

    public static AssertResponseContent assertThatResponseContent(MockMvcResponse response) {
        return assertThatResponseContent(response.mockHttpServletResponse());
    }

    public static AssertResponseContent assertThatResponseContent(MockHttpServletResponse response) {
        return new AssertResponseContent(response);
    }
}
