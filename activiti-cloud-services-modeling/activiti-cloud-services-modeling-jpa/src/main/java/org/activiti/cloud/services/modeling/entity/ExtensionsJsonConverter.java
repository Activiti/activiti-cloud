/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.modeling.entity;

import java.io.IOException;
import java.util.Map;

import javax.persistence.AttributeConverter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Json to model metadata converter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExtensionsJsonConverter implements AttributeConverter<Map, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map entity) {
        try {
            if (entity == null) {
                return null;
            }
            return getObjectMapper().writeValueAsString(entity);
        } catch (JsonProcessingException ex) {
            throw new DataIntegrityViolationException("Cannot convert entity to json data: " + entity,
                                                      ex);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String json) {
        try {
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return getObjectMapper().readValue(json,
                                               Map.class);
        } catch (IOException ex) {
            throw new DataRetrievalFailureException("Cannot convert the json data to entity: " + json,
                                                    ex);
        }
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
