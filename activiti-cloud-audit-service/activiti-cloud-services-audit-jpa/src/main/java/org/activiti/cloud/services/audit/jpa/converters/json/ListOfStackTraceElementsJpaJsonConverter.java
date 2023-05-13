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
package org.activiti.cloud.services.audit.jpa.converters.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.audit.api.AuditException;

public class ListOfStackTraceElementsJpaJsonConverter implements AttributeConverter<List<StackTraceElement>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<StackTraceElement> entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new AuditException("Unable to serialize object.", e);
        }
    }

    @Override
    public List<StackTraceElement> convertToEntityAttribute(String entityTextRepresentation) {
        try {
            if (entityTextRepresentation != null && entityTextRepresentation.length() > 0) {
                return objectMapper.readValue(
                    entityTextRepresentation,
                    new TypeReference<List<StackTraceElement>>() {}
                );
            } else {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new AuditException("Unable to deserialize object.", e);
        }
    }
}
