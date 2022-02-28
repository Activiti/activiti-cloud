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
package org.activiti.cloud.common.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SwaggerOperationIdTrimmerTest {

    private SwaggerOperationIdTrimmer operationIdTrimmer = new SwaggerOperationIdTrimmer();

    private final String DEFAULT_METHOD_NAME_WITH_HTTP_VERB_AND_NUMBER = "findAllUsingGET_2";
    private final String DEFAULT_METHOD_NAME_WITH_HTTP_VERB = "startUsingPOST";

    @Test
    void should_trimHttpVerbAndNumberInDefaultGeneratedMethods() {
        String trimmedMethodName =
                operationIdTrimmer.startingWith(DEFAULT_METHOD_NAME_WITH_HTTP_VERB_AND_NUMBER);
        assertThat(trimmedMethodName).isEqualTo("findAll");
    }

    @Test
    void should_trimHttpVerbInDefaultGeneratedMethods() {
        String trimmedMethodName =
                operationIdTrimmer.startingWith(DEFAULT_METHOD_NAME_WITH_HTTP_VERB);
        assertThat(trimmedMethodName).isEqualTo("start");
    }
}
