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

package org.activiti.cloud.common.error.attributes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorAttributesMessageSanitizerTest {

    private ErrorAttributesMessageSanitizer sanitizer = new ErrorAttributesMessageSanitizer();

    @ParameterizedTest
    @ValueSource(
        strings = {
            "An exception occurred @[java.lang.utils]",
            "Could not serialize @[javax.validation]",
            "Missing bean @[org.springframework]",
            "Error at com.google.guava",
            "Error at io.zipkin.brave",
            "Error at org._int.obscurepackage",
        }
    )
    void should_replaceMessageWithErrorNotDisclosed_when_matchesBlacklistedItems(String message) {
        Map<String, Object> errorAttributes = new HashMap<>();
        errorAttributes.put(ErrorAttributesMessageSanitizer.MESSAGE, message);

        errorAttributes = sanitizer.customize(errorAttributes, null);

        assertThat(errorAttributes)
            .containsEntry(
                ErrorAttributesMessageSanitizer.MESSAGE,
                ErrorAttributesMessageSanitizer.ERROR_NOT_DISCLOSED_MESSAGE
            );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "A model validation exception occurred.",
            "Visit domain.com/support.",
            "Status 418: I'm a teapot.",
            "Come visit us at domain.com.\nGoodbye!",
            "Come visit us at domain.com. Goodbye!",
            "Learn java.",
            "Learn java. and stuff",
            "Please visit support.domain.com.",
            "Somejava.com is not a real site",
        }
    )
    void should_letMessagePass_when_doesNotMatchBlacklistedItems(String message) {
        Map<String, Object> errorAttributes = new HashMap<>();
        errorAttributes.put(ErrorAttributesMessageSanitizer.MESSAGE, message);

        errorAttributes = sanitizer.customize(errorAttributes, null);

        assertThat(errorAttributes).containsEntry(ErrorAttributesMessageSanitizer.MESSAGE, message);
    }
}
