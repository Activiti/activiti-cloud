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
package org.activiti.cloud.services.notifications.graphql.ws.util;


import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Json Converter class
 */
public class JsonConverter {
    //
    // This is important because the graphql spec says that null values should be present
    //
    static final ObjectMapper JSON = new ObjectMapper()
    		.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
    		.setSerializationInclusion(Include.ALWAYS);

    public static Map<String, Object> toMap(String jsonStr) throws JsonParseException, JsonMappingException, IOException {
        if (jsonStr == null || jsonStr.trim().length() == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = JSON.readValue(jsonStr, new TypeReference<Map<String,Object>>() {});

        return map == null ? Collections.emptyMap() : map;
    }

    public static String toJsonString(Object obj) throws JsonProcessingException {
        return JSON.writeValueAsString(obj);
    }
}
