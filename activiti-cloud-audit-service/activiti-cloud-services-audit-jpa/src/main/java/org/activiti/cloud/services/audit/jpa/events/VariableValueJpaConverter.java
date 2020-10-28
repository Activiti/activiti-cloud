package org.activiti.cloud.services.audit.jpa.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.persistence.AttributeConverter;
import org.activiti.cloud.services.audit.api.AuditException;

public class VariableValueJpaConverter implements AttributeConverter<VariableValue<?>, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

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
