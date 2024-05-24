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
package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class MapOfStringObjectJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public MapOfStringObjectJsonConverter() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(Map<String, Object> variableValue) {
        try {
            return objectMapper.writeValueAsString(variableValue);
        } catch (JsonProcessingException e) {
            throw new QueryException("Unable to serialize list of map of string objects", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            if (dbData != null && dbData.length() > 0) {
                return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
            } else {
                return Collections.emptyMap();
            }
        } catch (IOException e) {
            throw new QueryException("Unable to deserialize map of string objects", e);
        }
    }
}
