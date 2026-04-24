/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.acc.shared.rest.httpclient;

import java.io.IOException;
import org.activiti.cloud.acc.shared.model.AuthToken;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class Serializer {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    public static AuthToken toAuthToken(String token) throws IOException {
        return objectMapper.readValue(token, AuthToken.class);
    }

    public static String toJsonString(Object object) throws JacksonException {
        return objectMapper.writeValueAsString(object);
    }
}
