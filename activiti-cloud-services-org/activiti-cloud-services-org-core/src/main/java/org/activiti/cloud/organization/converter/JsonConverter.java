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

package org.activiti.cloud.organization.converter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generic json converter
 */
public class JsonConverter<T> {

    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);

    private final Class<T> entityClass;

    private final ObjectMapper objectMapper;

    public JsonConverter(Class<T> entityClass,
                         ObjectMapper objectMapper) {
        this.entityClass = entityClass;
        this.objectMapper = objectMapper;
    }

    public String convertToJson(T entity) {
        return convertToJson(entity,
                             null);
    }

    public String convertToJson(T entity,
                                Class<?> view) {
        try {
            if (entity == null) {
                return null;
            }

            ObjectMapper objectMapper = getObjectMapper();
            return Optional.ofNullable(view)
                    .map(objectMapper::writerWithView)
                    .orElseGet(objectMapper::writer)
                    .writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert entity to json: " + entity,
                         e);
            throw new ModelingException("Cannot convert entity to json: " + entity,
                                        e);
        }
    }

    public byte[] convertToJsonBytes(T entity) {
        return convertToJsonBytes(entity,
                                  null);
    }

    public byte[] convertToJsonBytes(T entity,
                                     Class<?> view) {
        try {
            if (entity == null) {
                return null;
            }

            ObjectMapper objectMapper = getObjectMapper();
            return Optional.ofNullable(view)
                    .map(objectMapper::writerWithView)
                    .orElseGet(objectMapper::writer)
                    .writeValueAsBytes(entity);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert entity to json: " + entity,
                         e);
            throw new ModelingException("Cannot convert entity to json: " + entity,
                                        e);
        }
    }

    public T convertToEntity(byte[] json) {
        return convertToEntity(json,
                               null);
    }

    public T convertToEntity(byte[] json,
                             Class<?> view) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return convertToEntity(new String(json,
                                          UTF_8),
                               view);
    }

    public T convertToEntity(String json) {
        return convertToEntity(json,
                               null);
    }

    public T convertToEntity(String json,
                             Class<?> view) {
        try {
            if (StringUtils.isEmpty(json)) {
                return null;
            }

            ObjectMapper objectMapper = getObjectMapper();
            return Optional.ofNullable(view)
                    .map(objectMapper::readerWithView)
                    .orElseGet(objectMapper::reader)
                    .forType(getEntityClass())
                    .readValue(json);
        } catch (IOException e) {
            logger.error("Cannot convert json to entity: " + json,
                         e);
            throw new ModelingException("Cannot convert json to entity: " + json,
                                        e);
        }
    }

    public Optional<T> tryConvertToEntity(String json) {
        try {
            return Optional.of(convertToEntity(json));
        } catch (ModelingException e) {
            return Optional.empty();
        }
    }

    public Optional<T> tryConvertToEntity(byte[] json) {
        try {
            return Optional.of(convertToEntity(json));
        } catch (ModelingException e) {
            return Optional.empty();
        }
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected Class<T> getEntityClass() {
        return entityClass;
    }
    
    public Optional<T> tryConvertToEntity(Map jsonMap) {
        if(jsonMap==null || jsonMap.isEmpty()) {
            return Optional.empty();
        }
        try {
            String json = objectMapper.writeValueAsString(jsonMap);
            return this.tryConvertToEntity(json);
        } catch (ModelingException | JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
