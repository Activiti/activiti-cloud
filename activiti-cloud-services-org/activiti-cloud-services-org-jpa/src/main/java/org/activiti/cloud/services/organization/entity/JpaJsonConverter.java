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

package org.activiti.cloud.services.organization.entity;

import java.io.IOException;
import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.util.StringUtils;

/**
 * Jpa converter from an to json string
 */
public abstract class JpaJsonConverter<T> implements AttributeConverter<T, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(T entity) {
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
    public T convertToEntityAttribute(String json) {
        try {
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return getObjectMapper().readValue(json,
                                               getEntityClass());
        } catch (IOException ex) {
            throw new DataRetrievalFailureException("Cannot convert the json data to entity: " + json,
                                                    ex);
        }
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected abstract Class<T> getEntityClass();
}
