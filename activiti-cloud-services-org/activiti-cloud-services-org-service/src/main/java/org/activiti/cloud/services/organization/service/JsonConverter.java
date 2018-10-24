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

package org.activiti.cloud.services.organization.service;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

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
        try {
            if (entity == null) {
                return null;
            }

            return getObjectMapper().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert entity to json: " + entity,
                         e);
            throw new ModelingException("Cannot convert entity to json: " + entity,
                                        e);
        }
    }

    public byte[] convertToJsonBytes(T entity) {
        try {
            if (entity == null) {
                return null;
            }

            return getObjectMapper().writeValueAsBytes(entity);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert entity to json: " + entity,
                         e);
            throw new ModelingException("Cannot convert entity to json: " + entity,
                                        e);
        }
    }

    public T convertToEntity(byte[] json) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return convertToEntity(new String(json));
    }

    public T convertToEntity(String json) {
        try {
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return getObjectMapper().readValue(json,
                                               getEntityClass());
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
}
