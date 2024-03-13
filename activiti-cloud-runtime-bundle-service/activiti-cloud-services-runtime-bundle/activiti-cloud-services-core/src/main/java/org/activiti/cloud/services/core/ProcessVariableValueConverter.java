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
package org.activiti.cloud.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.engine.ActivitiException;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;

public class ProcessVariableValueConverter {

    private static final Map<String, Class<?>> typeRegistry = new HashMap<>();

    static {
        typeRegistry.put("string", String.class);
        typeRegistry.put("long", Long.class);
        typeRegistry.put("int", Integer.class);
        typeRegistry.put("integer", Integer.class);
        typeRegistry.put("boolean", Boolean.class);
        typeRegistry.put("double", Double.class);
        typeRegistry.put("date", Date.class);
        typeRegistry.put("localdate", LocalDate.class);
        typeRegistry.put("bigdecimal", BigDecimal.class);
        typeRegistry.put("json", JsonNode.class);
    }

    private final ConversionService conversionService;

    public ProcessVariableValueConverter(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(ProcessVariableValue variableValue) {
        Class<?> type = typeRegistry.getOrDefault(variableValue.getType().toLowerCase(), Object.class);
        try {
            return (T) this.conversionService.convert(variableValue.getValue(), type);
        } catch (Exception ex) {
            throw new ActivitiException("VariableValue conversion error", ex);
        }
    }

    public boolean canConvert(String type) {
        return typeRegistry.containsKey(type);
    }
}
