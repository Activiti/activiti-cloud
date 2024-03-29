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
package org.activiti.cloud.services.notifications.graphql.ws.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.services.notifications.graphql.ws.util.JsonConverter;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {

    @Test
    public void testToMapEmptyString() throws JsonParseException, JsonMappingException, IOException {
        // given
        String jsonStr = "";

        // when
        Map<String, Object> result = JsonConverter.toMap(jsonStr);

        // then
        assertThat(result).isEqualTo(Collections.emptyMap());
    }

    @Test
    public void testToMapNullValue() throws JsonParseException, JsonMappingException, IOException {
        // given
        String jsonStr = "{\"key\":null}";

        // when
        Map<String, Object> result = JsonConverter.toMap(jsonStr);

        // then
        assertThat(result).containsKey("key");
        assertThat(result).containsValue(null);
    }

    @Test
    public void testToJsonStringNullValues() throws JsonProcessingException {
        // given
        Map<String, Object> obj = new HashMap<String, Object>();

        obj.put("key", null);

        // when
        String result = JsonConverter.toJsonString(obj);

        // then
        assertThat(result).isEqualTo("{\"key\":null}");
    }
}
