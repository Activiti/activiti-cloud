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
package org.activiti.cloud.services.audit.jpa.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.persistence.AttributeConverter;
import org.activiti.cloud.services.audit.api.AuditException;

public class VariableValueJpaConverter implements AttributeConverter<VariableValue<?>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(VariableValue<?> entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new AuditException("Unable to serialize object.", e);
        }
    }

    @Override
    public VariableValue<?> convertToEntityAttribute(String entityTextRepresentation) {
        try {
            if (entityTextRepresentation != null && entityTextRepresentation.length() > 0) {
                return objectMapper.readValue(entityTextRepresentation, VariableValue.class);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new AuditException("Unable to deserialize object.", e);
        }
    }
}
